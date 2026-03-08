package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.core.ai.ChatCancellationRegistry;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.ChatContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.SignalType;

import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatStreamLifecycleHandler {

    private final ChatCancellationRegistry chatCancellationRegistry;
    private final ChatPersistenceService chatPersistenceService;
    private final ChatCitationService chatCitationService;
    private final ChatTurnPostProcessor chatTurnPostProcessor;

    public void handle(SignalType signalType,
                       String generationId,
                       ChatContext context,
                       StringBuilder fullResponse,
                       Long turnNumber,
                       AtomicReference<ChatClientResponse> lastClientResponse,
                       boolean webSearchRequested) {
        boolean userCancelled = chatCancellationRegistry.isManuallyCancelled(generationId);
        chatCancellationRegistry.clear(generationId);

        String threadId = context.chatThread().getId();
        if (userCancelled) {
            handleCancel(generationId, threadId, context, fullResponse, turnNumber);
            return;
        }

        switch (signalType) {
            case ON_COMPLETE -> handleOnComplete(
                    generationId,
                    threadId,
                    context,
                    fullResponse,
                    turnNumber,
                    lastClientResponse,
                    webSearchRequested
            );
            case ON_ERROR ->
                    log.warn("Streaming ERROR for threadId: {}, generationId: {}", threadId, generationId);
            case CANCEL ->
                    handleCancel(generationId, threadId, context, fullResponse, turnNumber);
            default ->
                    log.debug("Streaming finished with {} for generationId: {}", signalType, generationId);
        }
    }

    private void handleCancel(String generationId,
                              String threadId,
                              ChatContext context,
                              StringBuilder fullResponse,
                              Long turnNumber) {
        if (!fullResponse.isEmpty()) {
            chatPersistenceService.saveAssistantMessage(
                    context.chatThread(),
                    turnNumber,
                    fullResponse.toString(),
                    null
            );
            chatTurnPostProcessor.triggerSummaryIfNeeded(threadId, turnNumber);
        }
        log.info("Streaming response CANCELLED for threadId: {}, generationId: {}", threadId, generationId);
    }

    private void handleOnComplete(String generationId,
                                  String threadId,
                                  ChatContext context,
                                  StringBuilder fullResponse,
                                  Long turnNumber,
                                  AtomicReference<ChatClientResponse> lastClientResponse,
                                  boolean webSearchRequested) {
        if (!fullResponse.isEmpty()) {
            var citations = chatCitationService.build(
                    context.ragDocuments(),
                    lastClientResponse.get(),
                    webSearchRequested
            );
            log.debug("Stream citations for threadId {}: {}", threadId, citations);
            chatPersistenceService.saveAssistantMessage(
                    context.chatThread(),
                    turnNumber,
                    fullResponse.toString(),
                    citations
            );
            log.info("Streaming response completed for threadId: {}, generationId: {}", threadId, generationId);
        } else {
            log.info("Streaming completed with empty response for generationId: {}", generationId);
        }

        chatTurnPostProcessor.onAssistantResponseCompleted(context, turnNumber);
    }
}
