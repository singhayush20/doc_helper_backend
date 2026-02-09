package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryLlmResponse;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryTone;
import com.ayushsingh.doc_helper.features.doc_summary.prompt.SummaryPromptBuilder;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentTokenEstimationService;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryGenerationResult;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryGenerationService;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryLlmService;
import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryGenerationServiceImpl implements SummaryGenerationService {

    private static final int MAX_RETRIES = 2;
    private static final Duration LLM_TIMEOUT = Duration.ofSeconds(45);
    private static final int TOKEN_SAFETY_BUFFER = 50;

    private final SummaryLlmService llmService;
    private final DocumentTokenEstimationService tokenEstimator;

    @Override
    public SummaryGenerationResult generate(
            List<String> chunks,
            SummaryTone tone,
            SummaryLength length,
            long remainingTokens,
            LongConsumer tokenConsumer
    ) {
        LongConsumer usageConsumer = tokenConsumer != null ? tokenConsumer : tokens -> {};
        List<String> normalizedChunks = normalizeChunks(chunks);
        if (normalizedChunks.isEmpty()) {
            throw new BaseException("Document content is empty", ExceptionCodes.DOCUMENT_PARSING_FAILED);
        }

        List<String> chunkSummaries = new ArrayList<>();
        long totalTokens = 0;

        for (String chunk : normalizedChunks) {
            String prompt = SummaryPromptBuilder.buildChunkPrompt(chunk, tone, length);
            int maxOutputTokens = resolveMaxOutputTokens(prompt, length, false, remainingTokens);
            SummaryLlmResponse response = callWithRetry(prompt, maxOutputTokens);

            chunkSummaries.add(response.content());

            long usedTokens = resolveTokensUsed(response, prompt);
            totalTokens += usedTokens;
            remainingTokens = updateRemainingTokens(remainingTokens, usedTokens);
            usageConsumer.accept(usedTokens);
        }

        List<String> current = chunkSummaries;
        while (current.size() > 1) {
            AggregationResult agg = aggregateLevel(current, tone, length, false, remainingTokens, usageConsumer);
            current = agg.summaries();
            totalTokens += agg.tokensUsed();
            remainingTokens = agg.remainingTokens();
        }

        String finalPrompt = SummaryPromptBuilder.buildAggregatePrompt(
                current, tone, length, true);
        int finalMaxOutputTokens = resolveMaxOutputTokens(finalPrompt, length, true, remainingTokens);
        SummaryLlmResponse finalResponse = callWithRetry(finalPrompt, finalMaxOutputTokens);
        String finalSummary = finalResponse.content();
        long finalTokens = resolveTokensUsed(finalResponse, finalPrompt);
        totalTokens += finalTokens;
        usageConsumer.accept(finalTokens);

        return new SummaryGenerationResult(finalSummary, Math.toIntExact(totalTokens));
    }

    @Override
    public long estimateTokens(List<String> chunks) {
        return normalizeChunks(chunks).stream()
                .mapToLong(tokenEstimator::estimateTokens)
                .sum();
    }

    private AggregationResult aggregateLevel(
            List<String> summaries,
            SummaryTone tone,
            SummaryLength length,
            boolean finalPass,
            long remainingTokens,
            LongConsumer usageConsumer
    ) {
        int groupSize = switch (length) {
            case SHORT -> 6;
            case MEDIUM -> 4;
            case LONG -> 3;
        };

        List<String> next = new ArrayList<>();
        long usedTokens = 0;
        for (int i = 0; i < summaries.size(); i += groupSize) {
            List<String> group =
                    summaries.subList(i, Math.min(i + groupSize, summaries.size()));
            String prompt = SummaryPromptBuilder.buildAggregatePrompt(group, tone, length, finalPass);
            int maxOutputTokens = resolveMaxOutputTokens(prompt, length, finalPass, remainingTokens);
            SummaryLlmResponse response = callWithRetry(prompt, maxOutputTokens);
            next.add(response.content());

            long callTokens = resolveTokensUsed(response, prompt);
            usedTokens += callTokens;
            remainingTokens = updateRemainingTokens(remainingTokens, callTokens);
            usageConsumer.accept(callTokens);
        }
        return new AggregationResult(next, usedTokens, remainingTokens);
    }

    private SummaryLlmResponse callWithRetry(String prompt, int maxOutputTokens) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            try {
                return CompletableFuture.supplyAsync(() -> llmService.generate(prompt, maxOutputTokens))
                        .orTimeout(LLM_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                        .join();
            } catch (RuntimeException e) {
                last = e;
                log.warn("LLM call failed (attempt {}/{}): {}",
                        attempt, MAX_RETRIES + 1, e.getMessage());
                if (attempt <= MAX_RETRIES) {
                    try {
                        Thread.sleep(200L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw last != null ? last : new RuntimeException("LLM call failed");
    }

    private List<String> normalizeChunks(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            String value = normalize(chunk);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        normalized = normalized.replaceAll("[ \\t]+", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    private int resolveMaxOutputTokens(
            String prompt,
            SummaryLength length,
            boolean finalPass,
            long remainingTokens
    ) {
        int promptTokens = tokenEstimator.estimateTokens(prompt);
        int desiredMax = switch (length) {
            case SHORT -> finalPass ? 300 : 180;
            case MEDIUM -> finalPass ? 500 : 250;
            case LONG -> finalPass ? 800 : 400;
        };

        long allowed = remainingTokens - promptTokens - TOKEN_SAFETY_BUFFER;
        if (allowed <= 0) {
            throw new BaseException("Quota exceeded", ExceptionCodes.QUOTA_EXCEEDED);
        }

        long maxTokens = Math.min(allowed, desiredMax);
        return (int) Math.max(1, maxTokens);
    }

    private long resolveTokensUsed(SummaryLlmResponse response, String prompt) {
        if (response.totalTokens() != null && response.totalTokens() > 0) {
            return response.totalTokens();
        }

        int promptTokens = tokenEstimator.estimateTokens(prompt);
        int responseTokens = tokenEstimator.estimateTokens(response.content());
        return (long) promptTokens + responseTokens;
    }

    private long updateRemainingTokens(long remainingTokens, long usedTokens) {
        long updated = remainingTokens - usedTokens;
        if (updated < 0) {
            throw new BaseException("Quota exceeded", ExceptionCodes.QUOTA_EXCEEDED);
        }
        return updated;
    }

    private record AggregationResult(
            List<String> summaries,
            long tokensUsed,
            long remainingTokens
    ) {
    }
}
