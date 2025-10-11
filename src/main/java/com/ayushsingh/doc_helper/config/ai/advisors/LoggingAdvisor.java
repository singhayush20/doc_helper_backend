package com.ayushsingh.doc_helper.config.ai.advisors;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.config.security.UserContext;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.TokenUsageDto;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.ChatOperationType;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.TokenUsageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingAdvisor implements CallAdvisor, StreamAdvisor {

    private final TokenUsageService tokenUsageService;

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    @NonNull
    public String getName() {
        return "TokenUsageLoggingAdvisor";
    }

    @Override
    @NonNull
    public ChatClientResponse adviseCall(
            @NonNull ChatClientRequest chatClientRequest,
            @NonNull CallAdvisorChain callAdvisorChain) {
        Instant startTime = Instant.now();

        log.debug("===AI Call Started===");
        log.debug("Request timestamp: {}", startTime);

        try {
            ChatClientResponse response = callAdvisorChain.nextCall(
                    chatClientRequest);

            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            log.debug("=== AI Call Completed ===");
            log.debug("Response timestamp: {}", endTime);
            log.debug("Total duration: {} ms", duration.toMillis());
            logResponseDetails(response.chatResponse());

            persistUsageMetrics(chatClientRequest, response, duration, ChatOperationType.CHAT_CALL);

            return response;
        } catch (Exception e) {
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            log.error("=== AI Call Failed ===");
            log.error("Error timestamp: {}", endTime);
            log.error("Duration before failure: {} ms", duration.toMillis());
            log.error("Error details: ", e);

            throw e;
        }
    }

    @NonNull
    @Override
    public Flux<ChatClientResponse> adviseStream(
            @NonNull ChatClientRequest chatClientRequest,
            StreamAdvisorChain streamAdvisorChain) {
        Instant startTime = Instant.now();
        AtomicReference<ChatClientResponse> lastResponse = new AtomicReference<>();
        AtomicInteger chunkCount = new AtomicInteger(0);

        log.info("=== AI Stream Started ===");
        log.info("Stream request timestamp: {}", startTime);
        logRequestDetails(chatClientRequest);

        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnNext(response -> {
                    // Capture each chunk
                    lastResponse.set(response);
                    chunkCount.incrementAndGet();

                    ChatResponse chatResponse = response.chatResponse();
                    if (chatResponse != null && chatResponse.getMetadata() != null
                            && chatResponse.getMetadata().getModel() != null) {
                        log.debug("Chunk {} model: {}", chunkCount.get(), chatResponse.getMetadata().getModel());
                    }
                    logResponseDetails(chatResponse);
                })
                .doOnComplete(() -> {
                    Instant endTime = Instant.now();
                    Duration duration = Duration.between(startTime, endTime);

                    log.info("=== AI Stream Completed ===");
                    log.info("Stream completion timestamp: {}", endTime);
                    log.info("Stream total duration: {} ms",
                            duration.toMillis());
                    log.info("Total chunks processed: {}", chunkCount.get());

                    // Log final usage from the last response
                    ChatClientResponse finalResponse = lastResponse.get();
                    if (finalResponse != null &&
                            finalResponse.chatResponse() != null) {
                        logResponseDetails(finalResponse.chatResponse());
                        persistUsageMetrics(chatClientRequest,
                                finalResponse, duration, ChatOperationType.CHAT_STREAM);
                    }
                })
                .doOnError(error -> {
                    Instant endTime = Instant.now();
                    Duration duration = Duration.between(startTime, endTime);
                    log.error("=== AI Stream Failed ===");
                    log.error("Stream error timestamp: {}", endTime);
                    log.error("Stream duration before failure: {} ms",
                            duration.toMillis());
                    log.error("Chunks processed before error: {}",
                            chunkCount.get());
                    log.error("Stream error details: ", error);
                });
    }

    private void persistUsageMetrics(
            ChatClientRequest request,
            ChatClientResponse response,
            Duration duration,
            ChatOperationType chatResponseType) {
        try {
            ChatResponse chatResponse = response.chatResponse();
            if (chatResponse == null || chatResponse.getMetadata() == null) {
                log.warn("No metadata available to persist usage");
                return;
            }

            Usage usage = chatResponse.getMetadata().getUsage();
            if (usage == null) {
                log.warn("No usage information available");
                return;
            }

            Long userId = extractUserId();
            Long documentId = extractDocumentId(request);
            String threadId = extractThreadId(request);
            String messageId = extractMessageId(response);
            String modelName = chatResponse.getMetadata().getModel();

            TokenUsageDto usageDTO = TokenUsageDto.builder()
                    .userId(userId)
                    .documentId(documentId)
                    .threadId(threadId)
                    .messageId(messageId)
                    .promptTokens(usage.getPromptTokens().longValue())
                    .completionTokens(usage.getCompletionTokens().longValue())
                    .totalTokens(usage.getTotalTokens().longValue())
                    .modelName(modelName)
                    .operationType(chatResponseType)
                    .durationMs(duration.toMillis())
                    .build();

            tokenUsageService.recordTokenUsage(usageDTO);

            log.debug("Successfully persisted token usage to DB: userId={}, " +
                     "tokens={}",
                    userId, usage.getTotalTokens());

        } catch (Exception e) {
            log.error("Failed to persist usage metrics to DB", e);
        }
    }

    /**
     * Extract user ID from security context
     */
    private Long extractUserId() {
        try {
            return UserContext.getCurrentUser().getUser().getId();
        } catch (Exception e) {
            log.warn("Could not extract userId from UserContext", e);
            return null;
        }
    }

    private Long extractDocumentId(ChatClientRequest request) {
        try {
            Object docId = request.context().get("documentId");
            if (docId instanceof Long) {
                return (Long) docId;
            } else if (docId instanceof Number) {
                return ((Number) docId).longValue();
            } else if (docId instanceof String) {
                return Long.parseLong((String) docId);
            }
        } catch (Exception e) {
            log.debug("Could not extract documentId from request context", e);
        }
        return null;
    }

    private String extractThreadId(ChatClientRequest request) {
        try {
            Object threadId = request.context().get("threadId");
            if (threadId != null) {
                return threadId.toString();
            }
        } catch (Exception e) {
            log.debug("Could not extract threadId from request context", e);
        }
        return null;
    }

    private String extractMessageId(ChatClientResponse response) {
        try {
            if (response.chatResponse() != null &&
                    response.chatResponse().getResult() != null &&
                    response.chatResponse().getResult().getMetadata() != null) {

                Object msgId = response.chatResponse().getResult()
                        .getMetadata().get("messageId");
                if (msgId != null) {
                    return msgId.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract messageId from response", e);
        }
        return null;
    }

    private void logRequestDetails(ChatClientRequest request) {
        log.debug("Request details:");
        log.debug("- Prompt messages count: {}",
                request.prompt().getInstructions().size());

        // Log the actual prompt content (be careful with sensitive data)
        request.prompt()
                .getInstructions()
                .forEach(message -> log.info(
                        "- Message type: {}, Content length: {}",
                        message.getClass().getSimpleName(),
                        message.getText().length()));

        log.debug("- Context keys: {}", request.context().keySet());
    }

    private void logResponseDetails(ChatResponse chatResponse) {
        if (chatResponse != null && chatResponse.getMetadata() != null) {
            Usage usage = chatResponse.getMetadata().getUsage();
            if (usage != null) {
                log.debug("=== Token Usage Summary ===");
                log.debug("Prompt tokens: {} | Completion tokens: {} | Total: {}",
                        usage.getPromptTokens(),
                        usage.getCompletionTokens(),
                        usage.getTotalTokens());

                Object nativeUsage = usage.getNativeUsage();
                if (nativeUsage != null) {
                    String nativeStr = nativeUsage.toString();
                    if (nativeStr.contains("promptTokensDetails=") &&
                            !nativeStr.contains("promptTokensDetails=null")) {
                        log.debug("Additional usage details: {}", nativeUsage);
                    } else {
                        log.debug("Native usage available (no additional details)");
                    }
                }
            } else {
                log.warn("No usage metadata available in response");
            }

            if (chatResponse.getMetadata().getModel() != null) {
                log.debug("Model used: {}", chatResponse.getMetadata().getModel());
            }
        }
    }
}
