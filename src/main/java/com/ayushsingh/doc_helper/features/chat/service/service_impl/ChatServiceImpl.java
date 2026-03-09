package com.ayushsingh.doc_helper.features.chat.service.service_impl;

import com.ayushsingh.doc_helper.core.ai.ChatCancellationRegistry;
import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatHistoryResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.entity.TurnReservation;
import com.ayushsingh.doc_helper.features.chat.service.ChatCitationService;
import com.ayushsingh.doc_helper.features.chat.service.ChatClientRequestFactory;
import com.ayushsingh.doc_helper.features.chat.service.ChatContextFactory;
import com.ayushsingh.doc_helper.features.chat.service.ChatPersistenceService;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;
import com.ayushsingh.doc_helper.features.chat.service.ChatStreamLifecycleHandler;
import com.ayushsingh.doc_helper.features.chat.service.ChatTurnPostProcessor;
import com.ayushsingh.doc_helper.features.chat.service.ThreadTurnService;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.ChatContext;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public Flux<ServerSentEvent<ChatCallResponse>> generateStreamingResponse(ChatRequest chatRequest,
                                                                             Boolean webSearch,
                                                                             String generationId) {
        log.debug("Generating streaming response for documentId: {}, generationId: {}",
                chatRequest.documentId(), generationId);

        Long userId = UserContext.getCurrentUser().getUser().getId();
        quotaManagementService.checkAndEnforceQuota(userId, DEFAULT_TOKEN_THRESHOLD);

        boolean webSearchRequested = Boolean.TRUE.equals(webSearch);
        ChatContext context = chatContextFactory.prepare(chatRequest, webSearchRequested);
        String threadId = context.chatThread().getId();

        TurnReservation reservation = threadTurnService.reserveTurn(threadId);
        Long turnNumber = reservation.turnNumber();
        chatPersistenceService.saveUserMessage(context.chatThread(), turnNumber, chatRequest.question());

        StringBuilder fullResponse = new StringBuilder();
        Flux<Void> cancelFlux = chatCancellationRegistry.getOrCreateCancelFlux(generationId);
        AtomicReference<ChatClientResponse> lastClientResponse = new AtomicReference<>();

        Flux<ServerSentEvent<ChatCallResponse>> tokenFlux = chatClientRequestFactory
                .build(context, webSearchRequested, generationId)
                .stream()
                .chatClientResponse()
                .takeUntilOther(cancelFlux)
                .doOnNext(clientResponse -> {
                    lastClientResponse.set(clientResponse);
                    ChatResponse chatResponse = clientResponse.chatResponse();
                    if (chatResponse != null) {
                        String token = chatResponse.getResult().getOutput().getText();
                        if (token != null) {
                            fullResponse.append(token);
                        }
                    }
                })
                .map(clientResponse -> {
                    ChatResponse chatResponse = clientResponse.chatResponse();
                    if (chatResponse == null) {
                        return ServerSentEvent.<ChatCallResponse>builder()
                                .event("token")
                                .build();
                    }

                    String token = chatResponse.getResult().getOutput().getText();
                    return ServerSentEvent.<ChatCallResponse>builder()
                            .event("token")
                            .data(new ChatCallResponse(token != null ? token : "", null, null, null))
                            .build();
                })
                .doOnError(error -> log.error(
                        "Error during streaming response for documentId: {}, generationId: {}",
                        chatRequest.documentId(), generationId, error))
                .doFinally(signalType -> chatStreamLifecycleHandler.handle(
                        signalType,
                        generationId,
                        context,
                        fullResponse,
                        turnNumber,
                        lastClientResponse,
                        webSearchRequested
                ));

        Flux<ServerSentEvent<ChatCallResponse>> citationsFlux = Mono
                .fromCallable(() -> chatCitationService.build(
                        context.ragDocuments(),
                        lastClientResponse.get(),
                        webSearchRequested
                ))
                .map(citations -> ServerSentEvent.<ChatCallResponse>builder()
                        .event("citations")
                        .data(new ChatCallResponse(null, citations, null, null))
                        .build())
                .flux();

        return tokenFlux.concatWith(citationsFlux);
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
                "document id: {} -> {}",chatRequest.documentId(),chatResponse);
        var citations = chatCitationService.build(
                context.ragDocuments(),
                clientResponse,
                webSearchRequested
        );

        chatPersistenceService.saveAssistantMessage(
                context.chatThread(),
                turnNumber,
                responseContent,
                citations
        );
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
}
