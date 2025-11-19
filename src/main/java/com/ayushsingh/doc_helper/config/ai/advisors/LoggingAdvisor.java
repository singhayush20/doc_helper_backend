package com.ayushsingh.doc_helper.config.ai.advisors;

import com.ayushsingh.doc_helper.features.usage_monitoring.dto.TokenUsageDto;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.ChatOperationType;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.TokenUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String ID = "id";
    private final TokenUsageService tokenUsageService;

    @Override
    public int getOrder() {
        return 0; // Execute FIRST to see all messages
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

        log.debug("=== AI Call Started ===");
        log.debug("Request timestamp: {}", startTime);

        try {
            // Get the response
            ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);

            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            log.debug("=== AI Call Completed ===");
            log.debug("Response timestamp: {}", endTime);
            log.debug("Total duration: {} ms", duration.toMillis());

            if (response.chatResponse() != null) {
                logResponseDetails(response.chatResponse());

                persistUsageMetrics(chatClientRequest, response, duration,
                        ChatOperationType.CHAT_CALL);
            }

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
                    System.out.println("New response: "+response.chatResponse().getResult().getOutput());
                    lastResponse.set(response);
                    int currentChunk = chunkCount.incrementAndGet();

                    ChatResponse chatResponse = response.chatResponse();
                    if (chatResponse != null) {

                        if (chatResponse.getMetadata() != null) {
                            Usage usage = chatResponse.getMetadata().getUsage();
                            Long totalTokens = usage != null && usage.getTotalTokens() != null
                                    ? usage.getTotalTokens()
                                    : 0L;

                            log.debug("Chunk {} - Model: {}, Total tokens: {} (cumulative)",
                                    currentChunk,
                                    chatResponse.getMetadata().getModel(),
                                    totalTokens);
                        }
                    }
                })
                .doOnComplete(() -> {
                    Instant endTime = Instant.now();
                    Duration duration = Duration.between(startTime, endTime);

                    log.debug("=== AI Stream Completed ===");
                    log.debug("Stream completion timestamp: {}", endTime);
                    log.debug("Stream total duration: {} ms", duration.toMillis());
                    log.debug("Total chunks processed: {}", chunkCount.get());

                    ChatClientResponse finalResponse = lastResponse.get();
                    if (finalResponse != null && finalResponse.chatResponse() != null) {
                        logResponseDetails(finalResponse.chatResponse());

                        persistUsageMetrics(chatClientRequest, finalResponse,
                                duration, ChatOperationType.CHAT_STREAM);
                    }

                    lastResponse.set(null);
                })
                .doOnError(error -> {
                    Instant endTime = Instant.now();
                    Duration duration = Duration.between(startTime, endTime);

                    log.error("=== AI Stream Failed ===");
                    log.error("Stream error timestamp: {}", endTime);
                    log.error("Stream duration before failure: {} ms", duration.toMillis());
                    log.error("Chunks processed before error: {}", chunkCount.get());
                    log.error("Stream error details: ", error);

                    lastResponse.set(null);
                });
    }

    private void persistUsageMetrics(ChatClientRequest request,
            ChatClientResponse response, Duration duration,
            ChatOperationType operationType) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            log.warn("No chat response available to persist usage");
            return;
        }

        Usage usage = extractUsage(chatResponse);
        if (usage == null || usage.getTotalTokens() == null ||
                usage.getTotalTokens() == 0) {
            log.warn(
                    "No valid usage information available. Usage object: {}",
                    usage);
            return;
        }

        Long userId = extractUserId(request);
        if (userId == null) {
            log.error("Cannot persist usage: userId is null");
            return;
        }

        Long documentId = extractDocumentId(request);
        String threadId = extractThreadId(request);
        String messageId = extractMessageId(chatResponse);

        String modelName = chatResponse.getMetadata() != null ? chatResponse.getMetadata().getModel() : "unknown";

        TokenUsageDto usageDTO = TokenUsageDto.builder()
                .userId(userId)
                .documentId(documentId)
                .threadId(threadId)
                .messageId(messageId)
                .promptTokens(usage.getPromptTokens() != null ? usage.getPromptTokens().longValue() : 0L)
                .completionTokens(
                        usage.getCompletionTokens() != null ? usage.getCompletionTokens().longValue() : 0L)
                .totalTokens(usage.getTotalTokens() != null ? usage.getTotalTokens().longValue() : 0L)
                .modelName(modelName)
                .operationType(operationType)
                .durationMs(duration.toMillis())
                .build();

        tokenUsageService.recordTokenUsage(usageDTO);

        log.info(
                "Successfully persisted token usage: userId={}, tokens={}, messageId={}",
                userId, usage.getTotalTokens(), messageId);
    }

    /**
     * Extract usage correctly from ChatResponse
     */
    private Usage extractUsage(ChatResponse chatResponse) {
        try {
            if (chatResponse.getMetadata() != null &&
                    chatResponse.getMetadata().getUsage() != null &&
                    chatResponse.getMetadata().getUsage().getTotalTokens() != null &&
                    chatResponse.getMetadata().getUsage().getTotalTokens() > 0) {
                return chatResponse.getMetadata().getUsage();
            }

            log.debug(
                    "No valid usage found in response. Metadata: {}, Results count: {}",
                    chatResponse.getMetadata(),
                    chatResponse.getResults() != null ? chatResponse.getResults().size() : 0);

        } catch (Exception e) {
            log.warn("Error extracting usage from response", e);
        }

        return null;
    }

    /**
     * Extract messageId correctly from ChatResponse
     */
    private String extractMessageId(ChatResponse chatResponse) {
        try {
            if (chatResponse.getMetadata() != null &&
                    chatResponse.getMetadata().getId() != null) {
                return chatResponse.getMetadata().getId();
            }

            if (chatResponse.getResults() != null &&
                    !chatResponse.getResults().isEmpty()) {
                var firstResult = chatResponse.getResults().getFirst();
                if (firstResult.getMetadata() != null) {
                    Object msgId = firstResult.getMetadata().get(ID);
                    if (msgId != null) {
                        return msgId.toString();
                    }
                }
            }

            String generatedId = "msg_" + System.currentTimeMillis() + "_" +
                    Integer.toHexString(chatResponse.hashCode());
            log.debug("No messageId found in response, generated: {}",
                    generatedId);
            return generatedId;

        } catch (Exception e) {
            log.warn("Error extracting messageId from response", e);
            return "msg_error_" + System.currentTimeMillis();
        }
    }

    /**
     * Extract user ID from request context
     */
    private Long extractUserId(ChatClientRequest request) {
        try {
            Object userId = request.context().get("userId");
            if (userId instanceof Long) {
                return (Long) userId;
            } else if (userId instanceof Number) {
                return ((Number) userId).longValue();
            } else if (userId instanceof String) {
                return Long.parseLong((String) userId);
            }
        } catch (Exception e) {
            log.debug("Could not extract userId from request context", e);
        }

        log.warn("No userId found in request context. Available keys: {}",
                request.context().keySet());
        return null;
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

    private void logRequestDetails(ChatClientRequest request) {
        log.debug("Request details:");
        log.debug("- Context keys: {}", request.context().keySet());

        if (request.prompt().getInstructions() != null) {
            log.debug("- Prompt messages count: {}",
                    request.prompt().getInstructions().size());

            request.prompt()
                    .getInstructions()
                    .forEach(message -> log.debug(
                            "- Message type: {}, Content length: {}",
                            message.getMessageType(),
                            message.getText() != null ? message.getText().length() : 0));
        }
    }

    private void logResponseDetails(ChatResponse chatResponse) {
        if (chatResponse == null) {
            log.warn("ChatResponse is null");
            return;
        }

        if (chatResponse.getMetadata() != null) {
            log.debug("=== Response Metadata ===");
            log.debug("Model: {}", chatResponse.getMetadata().getModel());
            log.debug("ID: {}", chatResponse.getMetadata().getId());

            Usage usage = extractUsage(chatResponse);
            if (usage != null && usage.getTotalTokens() != null &&
                    usage.getTotalTokens() > 0) {
                log.debug("=== Token Usage ===");
                log.debug("Prompt: {} | Generation: {} | Total: {}",
                        usage.getPromptTokens(), usage.getCompletionTokens(),
                        usage.getTotalTokens());
            } else {
                log.warn("No valid usage data in response");
            }
        }

        if (chatResponse.getResults() != null) {
            log.debug("Results count: {}", chatResponse.getResults().size());
        }
    }
}
