package com.ayushsingh.doc_helper.features.chat.service.service_impl;

import com.ayushsingh.doc_helper.core.ai.ChatCancellationRegistry;
import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatHistoryResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.entity.TurnReservation;
import com.ayushsingh.doc_helper.features.chat.service.*;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.ChatContext;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

        private static final Long DEFAULT_TOKEN_THRESHOLD = 6800L;

        private final QuotaManagementService quotaManagementService;
        private final ChatCancellationRegistry chatCancellationRegistry;
        private final ThreadTurnService threadTurnService;
        private final ChatContextFactory chatContextFactory;
        private final ChatPersistenceService chatPersistenceService;
        private final ChatClientRequestFactory chatClientRequestFactory;
        private final ChatCitationService chatCitationService;
        private final ChatStreamLifecycleHandler chatStreamLifecycleHandler;
        private final ChatTurnPostProcessor chatTurnPostProcessor;

        private enum ChatEventType {
                START("start"),
                MESSAGE("message"),
                CITATIONS("citations"),
                HEARTBEAT("heartbeat");

                ChatEventType(String value) {
                        this.value = value;
                }

                String getValue() {
                        return value;
                }

                private String value;
        }

        @Override
        public Flux<ServerSentEvent<ChatCallResponse>> generateStreamingResponse(
                        ChatRequest chatRequest,
                        Boolean webSearch,
                        String generationId) {

                log.debug("Generating streaming response for documentId: {}, generationId: {}",
                                chatRequest.documentId(), generationId);

                Long userId = UserContext.getCurrentUser().getUser().getId();
                quotaManagementService.checkAndEnforceQuota(userId, DEFAULT_TOKEN_THRESHOLD);

                boolean webSearchRequested = Boolean.TRUE.equals(webSearch);
                StreamingRequestState requestState = prepareStreamingRequest(chatRequest, webSearchRequested);

                StringBuilder fullResponse = new StringBuilder();
                Flux<Void> cancelFlux = chatCancellationRegistry.getOrCreateCancelFlux(generationId);
                AtomicReference<ChatClientResponse> lastClientResponse = new AtomicReference<>();

                Flux<ServerSentEvent<ChatCallResponse>> tokenFlux = createSharedTokenFlux(
                                chatRequest,
                                requestState,
                                webSearchRequested,
                                generationId,
                                fullResponse,
                                cancelFlux,
                                lastClientResponse);

                return createStartFlux()
                                .concatWith(Flux.merge(tokenFlux, createHeartbeatFlux(tokenFlux)))
                                .concatWith(createCitationsFlux(requestState.context(), webSearchRequested));
        }

        private StreamingRequestState prepareStreamingRequest(ChatRequest chatRequest,
                        boolean webSearchRequested) {
                ChatContext context = chatContextFactory.prepare(chatRequest, webSearchRequested);
                String threadId = context.chatThread().getId();
                TurnReservation reservation = threadTurnService.reserveTurn(threadId);
                Long turnNumber = reservation.turnNumber();

                chatPersistenceService.saveUserMessage(
                                context.chatThread(),
                                turnNumber,
                                chatRequest.question());

                return new StreamingRequestState(context, turnNumber);
        }

        private Flux<ServerSentEvent<ChatCallResponse>> createStartFlux() {
                return Flux.just(createEvent(
                                ChatEventType.START.getValue(),
                                new ChatCallResponse(null, null, null, null)));
        }

        private Flux<ServerSentEvent<ChatCallResponse>> createSharedTokenFlux(
                        ChatRequest chatRequest,
                        StreamingRequestState requestState,
                        boolean webSearchRequested,
                        String generationId,
                        StringBuilder fullResponse,
                        Flux<Void> cancelFlux,
                        AtomicReference<ChatClientResponse> lastClientResponse) {
                return createRawTokenFlux(
                                chatRequest,
                                requestState,
                                webSearchRequested,
                                generationId,
                                fullResponse,
                                cancelFlux,
                                lastClientResponse).publish().autoConnect(1);
        }

        private Flux<ServerSentEvent<ChatCallResponse>> createRawTokenFlux(
                        ChatRequest chatRequest,
                        StreamingRequestState requestState,
                        boolean webSearchRequested,
                        String generationId,
                        StringBuilder fullResponse,
                        Flux<Void> cancelFlux,
                        AtomicReference<ChatClientResponse> lastClientResponse) {
                return chatClientRequestFactory
                                .build(requestState.context(), webSearchRequested, generationId)
                                .stream()
                                .chatClientResponse()
                                .takeUntilOther(cancelFlux)
                                .doOnNext(clientResponse -> captureStreamingResponse(clientResponse, fullResponse,
                                                lastClientResponse))
                                .flatMap(this::createMessageEvent)
                                .doOnError(error -> log.error(
                                                "Error during streaming response for documentId: {}, generationId: {}",
                                                chatRequest.documentId(),
                                                generationId,
                                                error))
                                .doFinally(signalType -> chatStreamLifecycleHandler.handle(
                                                signalType,
                                                generationId,
                                                requestState.context(),
                                                fullResponse,
                                                requestState.turnNumber(),
                                                lastClientResponse,
                                                webSearchRequested));
        }

        private void captureStreamingResponse(ChatClientResponse clientResponse,
                        StringBuilder fullResponse,
                        AtomicReference<ChatClientResponse> lastClientResponse) {
                lastClientResponse.set(clientResponse);

                ChatResponse chatResponse = clientResponse.chatResponse();
                if (chatResponse == null) {
                        return;
                }

                var generation = chatResponse.getResult();
                String token = generation != null
                                ? generation.getOutput().getText()
                                : StringUtils.EMPTY;

                if (token != null && !token.isEmpty()) {
                        fullResponse.append(token);
                }
        }

        private Mono<ServerSentEvent<ChatCallResponse>> createMessageEvent(
                        ChatClientResponse clientResponse) {
                ChatResponse chatResponse = clientResponse.chatResponse();
                if (chatResponse == null) {
                        return Mono.empty();
                }

                var generation = chatResponse.getResult();
                if (generation == null) {
                        return Mono.empty();
                }

                String token = generation.getOutput().getText();
                if (token == null || token.isBlank()) {
                        return Mono.empty();
                }

                return Mono.just(createEvent(
                                ChatEventType.MESSAGE.getValue(),
                                new ChatCallResponse(token, null, null, null)));
        }

        private Flux<ServerSentEvent<ChatCallResponse>> createHeartbeatFlux(
                        Flux<ServerSentEvent<ChatCallResponse>> tokenFlux) {
                return Flux.interval(java.time.Duration.ofSeconds(10))
                                .map(tick -> createEvent(
                                                ChatEventType.HEARTBEAT
                                                                .getValue(),
                                                new ChatCallResponse(null, null, null, null)))
                                .takeUntilOther(tokenFlux.ignoreElements());
        }

        private Flux<ServerSentEvent<ChatCallResponse>> createCitationsFlux(
                        ChatContext context,
                        boolean webSearchRequested) {
                return Mono.fromCallable(() -> chatCitationService.build(
                                context.ragDocuments(),
                                webSearchRequested))
                                .map(citations -> createEvent(
                                                ChatEventType.CITATIONS.getValue(),
                                                new ChatCallResponse(null, citations, null, null)))
                                .flux();
        }

        private ServerSentEvent<ChatCallResponse> createEvent(String eventName,
                        ChatCallResponse data) {
                return ServerSentEvent.<ChatCallResponse>builder()
                                .event(eventName)
                                .data(data)
                                .build();
        }

        @Override
        public ChatCallResponse generateResponse(ChatRequest chatRequest,
                        Boolean webSearch) {
                Long userId = UserContext.getCurrentUser().getUser().getId();
                boolean webSearchRequested = Boolean.TRUE.equals(webSearch);
                ChatContext context = chatContextFactory.prepare(chatRequest, webSearchRequested);
                String threadId = context.chatThread().getId();
                TurnReservation reservation = threadTurnService.reserveTurn(threadId);
                Long turnNumber = reservation.turnNumber();

                quotaManagementService.checkAndEnforceQuota(userId, DEFAULT_TOKEN_THRESHOLD);

                log.debug("Generating non-streaming response for documentId: {}", chatRequest.documentId());
                chatPersistenceService.saveUserMessage(context.chatThread(), turnNumber, chatRequest.question());

                ChatClientResponse clientResponse = chatClientRequestFactory
                                .build(context, webSearchRequested, null)
                                .call()
                                .chatClientResponse();
                ChatResponse chatResponse = clientResponse.chatResponse();

                if (chatResponse == null) {
                        return new ChatCallResponse();
                }

                String responseContent = chatResponse.getResult().getOutput().getText();
                log.debug("Generated chat response content for non-streaming request " +
                                "for " +
                                "document id: {} -> {}", chatRequest.documentId(), chatResponse);
                var citations = chatCitationService.build(
                                context.ragDocuments(),
                                webSearchRequested);

                chatPersistenceService.saveAssistantMessage(
                                context.chatThread(),
                                turnNumber,
                                responseContent,
                                citations);
                chatTurnPostProcessor.onAssistantResponseCompleted(context, turnNumber);

                log.debug("Non-streaming response completed for threadId: {}", threadId);
                return ChatCallResponse.builder()
                                .message(responseContent)
                                .citations(citations)
                                .build();
        }

        @Override
        public ChatHistoryResponse fetchChatHistoryForDocument(Long documentId,
                        Integer page) {
                Long userId = UserContext.getCurrentUser().getUser().getId();
                return chatPersistenceService.fetchChatHistoryForDocument(documentId, userId, page);
        }

        @Override
        public Boolean deleteChatHistoryForDocument(Long documentId) {
                Long userId = UserContext.getCurrentUser().getUser().getId();
                return chatPersistenceService.deleteChatHistoryForDocument(documentId, userId);
        }

        private record StreamingRequestState(ChatContext context, Long turnNumber) {
        }
}
