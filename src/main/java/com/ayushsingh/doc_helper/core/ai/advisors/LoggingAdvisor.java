package com.ayushsingh.doc_helper.core.ai.advisors;

import com.ayushsingh.doc_helper.features.usage_monitoring.dto.TokenUsageDto;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.ChatOperationType;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.UsageRecordingService;

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
    private static final int STREAM_CHUNK_PREVIEW_LIMIT = 240;
    private static final int FINAL_RESPONSE_PREVIEW_LIMIT = 1200;
    private final UsageRecordingService usageRecordingService;

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
        String generationId = normalizeId(extractGenerationId(chatClientRequest));
        String threadId = normalizeId(extractThreadId(chatClientRequest));

        log.info("event=ai_call_start generationId={} threadId={} promptMessages={} contextKeys={}",
                generationId,
                threadId,
                promptMessageCount(chatClientRequest),
                chatClientRequest.context().keySet());

        try {
            ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);

            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            log.info("event=ai_call_complete generationId={} threadId={} durationMs={}",
                    generationId, threadId, duration.toMillis());

            if (response.chatResponse() != null) {
                logResponseDetails(response.chatResponse(), generationId);

                persistUsageMetrics(chatClientRequest, response, duration,
                        ChatOperationType.CHAT_CALL);
            }

            return response;
        } catch (Exception e) {
            Duration duration = Duration.between(startTime, Instant.now());
            log.error("event=ai_call_failed generationId={} threadId={} durationMs={}",
                    generationId, threadId, duration.toMillis(), e);

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
        String generationId = normalizeId(extractGenerationId(chatClientRequest));
        String threadId = normalizeId(extractThreadId(chatClientRequest));

        log.info("event=ai_stream_start generationId={} threadId={} promptMessages={}",
                generationId,
                threadId,
                promptMessageCount(chatClientRequest));
        logRequestDetails(chatClientRequest);

        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnCancel(() -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    log.info("event=ai_stream_cancelled generationId={} threadId={} durationMs={} chunks={}",
                            generationId, threadId, duration.toMillis(), chunkCount.get());

                    lastResponse.set(null);
                })
                .doOnNext(response -> {
                    lastResponse.set(response);
                    int currentChunk = chunkCount.incrementAndGet();

                    ChatResponse chatResponse = response.chatResponse();
                    if (chatResponse == null) {
                        log.debug("event=ai_stream_chunk generationId={} chunk={} chars=0",
                                generationId, currentChunk);
                        return;
                    }

                    String chunkContent = extractResponseContent(chatResponse);
                    if (chunkContent != null && !chunkContent.isBlank()) {
                        log.debug("event=ai_stream_chunk generationId={} chunk={} chars={} preview=\"{}\"",
                                generationId,
                                currentChunk,
                                chunkContent.length(),
                                toPreview(chunkContent, STREAM_CHUNK_PREVIEW_LIMIT));
                    } else {
                        log.debug("event=ai_stream_chunk generationId={} chunk={} chars=0",
                                generationId, currentChunk);
                    }

                    if (chatResponse.getMetadata() != null) {
                        Usage usage = chatResponse.getMetadata().getUsage();
                        Long totalTokens = usage != null && usage.getTotalTokens() != null
                                ? usage.getTotalTokens()
                                : 0L;

                        log.debug("event=ai_stream_chunk_meta generationId={} chunk={} model={} totalTokens={}",
                                generationId,
                                currentChunk,
                                chatResponse.getMetadata().getModel(),
                                totalTokens);
                    }
                })
                .doOnComplete(() -> {
                    Duration duration = Duration.between(startTime, Instant.now());

                    log.info("event=ai_stream_complete generationId={} threadId={} durationMs={} chunks={}",
                            generationId, threadId, duration.toMillis(), chunkCount.get());

                    ChatClientResponse finalResponse = lastResponse.get();
                    if (finalResponse != null && finalResponse.chatResponse() != null) {
                        logResponseDetails(finalResponse.chatResponse(), generationId);

                        persistUsageMetrics(chatClientRequest, finalResponse,
                                duration, ChatOperationType.CHAT_STREAM);
                    }

                    lastResponse.set(null);
                })
                .doOnError(error -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    log.error("event=ai_stream_failed generationId={} threadId={} durationMs={} chunks={}",
                            generationId, threadId, duration.toMillis(), chunkCount.get(), error);

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

        usageRecordingService.recordTokenUsage(usageDTO);

        log.info(
                "event=ai_usage_persisted userId={} operation={} tokens={} messageId={}",
                userId, operationType, usage.getTotalTokens(), messageId);
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
                    "event=ai_usage_missing metadata={} resultsCount={}",
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
            log.debug("event=ai_message_id_generated messageId={}",
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
            log.debug("event=ai_user_id_extraction_failed", e);
        }

        log.warn("event=ai_user_id_missing contextKeys={}",
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
            log.debug("event=ai_document_id_extraction_failed", e);
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
            log.debug("event=ai_thread_id_extraction_failed", e);
        }
        return null;
    }

    private String extractGenerationId(ChatClientRequest request) {
        try {
            Object genId = request.context().get("generationId");
            return genId != null ? genId.toString() : null;
        } catch (Exception e) {
            log.debug("event=ai_generation_id_extraction_failed", e);
            return null;
        }
    }

    private void logRequestDetails(ChatClientRequest request) {
        log.debug("event=ai_request_details contextKeys={} promptMessages={}",
                request.context().keySet(),
                promptMessageCount(request));

        if (request.prompt() != null && request.prompt().getInstructions() != null) {
            request.prompt()
                    .getInstructions()
                    .forEach(message -> log.debug(
                            "event=ai_request_message type={} contentChars={}",
                            message.getMessageType(),
                            message.getText() != null ? message.getText().length() : 0));
        }
    }

    private void logResponseDetails(ChatResponse chatResponse, String generationId) {
        if (chatResponse == null) {
            log.warn("ChatResponse is null");
            return;
        }

        String model = chatResponse.getMetadata() != null ? chatResponse.getMetadata().getModel() : "unknown";
        String messageId = extractMessageId(chatResponse);
        int resultsCount = chatResponse.getResults() != null ? chatResponse.getResults().size() : 0;

        log.info("event=ai_response_meta generationId={} model={} messageId={} results={}",
                generationId, model, messageId, resultsCount);

        if (chatResponse.getMetadata() != null) {
            Usage usage = extractUsage(chatResponse);
            if (usage != null && usage.getTotalTokens() != null &&
                    usage.getTotalTokens() > 0) {
                log.info("event=ai_response_usage generationId={} promptTokens={} completionTokens={} totalTokens={}",
                        generationId,
                        usage.getPromptTokens(), usage.getCompletionTokens(),
                        usage.getTotalTokens());
            } else {
                log.warn("No valid usage data in response");
            }
        }

        String responseContent = extractResponseContent(chatResponse);
        if (responseContent != null && !responseContent.isBlank()) {
            log.debug("event=ai_response_content generationId={} chars={} preview=\"{}\"",
                    generationId,
                    responseContent.length(),
                    toPreview(responseContent, FINAL_RESPONSE_PREVIEW_LIMIT));
        } else {
            log.debug("event=ai_response_content generationId={} chars=0", generationId);
        }
    }

    private int promptMessageCount(ChatClientRequest request) {
        if (request.prompt() == null || request.prompt().getInstructions() == null) {
            return 0;
        }
        return request.prompt().getInstructions().size();
    }

    private String normalizeId(String id) {
        return (id == null || id.isBlank()) ? "null" : id;
    }

    private String extractResponseContent(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return null;
        }

        try {
            if (chatResponse.getResult() != null &&
                    chatResponse.getResult().getOutput() != null &&
                    chatResponse.getResult().getOutput().getText() != null) {
                return chatResponse.getResult().getOutput().getText();
            }

            if (chatResponse.getResults() != null && !chatResponse.getResults().isEmpty()) {
                var firstResult = chatResponse.getResults().getFirst();
                if (firstResult.getOutput() != null) {
                    return firstResult.getOutput().getText();
                }
            }
        } catch (Exception e) {
            log.debug("event=ai_response_content_extraction_failed", e);
        }

        return null;
    }

    private String toPreview(String content, int maxLength) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String normalized = content.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength) + "...(truncated)";
    }
}
