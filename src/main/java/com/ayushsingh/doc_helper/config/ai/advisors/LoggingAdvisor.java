package com.ayushsingh.doc_helper.config.ai.advisors;

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
public class LoggingAdvisor implements CallAdvisor, StreamAdvisor {
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

        log.info("===AI Call Started===");
        log.info("Request timestamp: {}", startTime);

        try {
            ChatClientResponse response = callAdvisorChain.nextCall(
                    chatClientRequest);

            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            log.info("=== AI Call Completed ===");
            log.info("Response timestamp: {}", endTime);
            log.info("Total duration: {} ms", duration.toMillis());
            logResponseDetails(response.chatResponse());

            // TODO: Persist usage metrics

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
                    // TODO: Perform logging for each chunk if required
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
                        // TODO: Persist usage data
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

    private void logRequestDetails(ChatClientRequest request) {
        log.info("Request details:");
        log.info("- Prompt messages count: {}",
                request.prompt().getInstructions().size());

        // Log the actual prompt content (be careful with sensitive data)
        request.prompt()
                .getInstructions()
                .forEach(message -> log.info(
                        "- Message type: {}, Content length: {}",
                        message.getClass().getSimpleName(),
                        message.getText().length()));

        log.info("- Context keys: {}", request.context().keySet());
    }

    private void logResponseDetails(ChatResponse chatResponse) {
        if (chatResponse != null && chatResponse.getMetadata() != null) {
            Usage usage = chatResponse.getMetadata().getUsage();
            if (usage != null) {
                log.info("=== Token Usage Summary ===");
                log.info("Prompt tokens: {} | Completion tokens: {} | Total: {}",
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

            // Log model information if available
            if (chatResponse.getMetadata().getModel() != null) {
                log.info("Model used: {}", chatResponse.getMetadata().getModel());
            }
        }
    }
}
