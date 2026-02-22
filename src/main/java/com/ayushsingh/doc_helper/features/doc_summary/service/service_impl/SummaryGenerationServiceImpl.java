package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.features.doc_summary.dto.StructuredSummaryDto;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongConsumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryGenerationServiceImpl implements SummaryGenerationService {

    private static final int MIN_REQUIRED_TOKEN_DIFFERENCE = 80;
    private static final int MIN_OUTPUT_TOKENS = 120;

    private final SummaryLlmService llmService;
    private final DocumentTokenEstimationService tokenEstimator;

    @Value("${doc-summary.chunk.model:${doc-summary.model}}")
    private String chunkModel;

    @Value("${doc-summary.aggregate.model:${doc-summary.model}}")
    private String aggregateModel;

    @Value("${doc-summary.retry.max-retries:4}")
    private int maxRetries;

    @Value("${doc-summary.retry.base-delay-ms:300}")
    private long baseDelayMs;

    @Value("${doc-summary.retry.max-delay-ms:4000}")
    private long maxDelayMs;

    @Value("${doc-summary.merge.group-size:4}")
    private int mergeGroupSize;

    @Override
    public SummaryGenerationResult generate(
            List<String> chunks,
            SummaryTone tone,
            SummaryLength length,
            long remainingTokens,
            LongConsumer tokenConsumer) {

        LongConsumer usageConsumer = tokenConsumer != null ? tokenConsumer : t -> {
        };

        List<String> normalizedChunks = normalizeChunks(chunks);
        if (normalizedChunks.isEmpty()) {
            throw new BaseException(
                    "Document content is empty",
                    ExceptionCodes.DOCUMENT_PARSING_FAILED);
        }

        List<String> stageOutputs = new ArrayList<>();
        StructuredSummaryDto finalContent = null;
        long totalTokens = 0;

        for (String chunk : normalizedChunks) {
            String prompt = SummaryPromptBuilder.buildChunkPrompt(chunk, tone, length);
            int maxOutputTokens = resolveMaxOutputTokens(length, false, remainingTokens);

            SummaryLlmResponse response = callWithRetry(prompt, maxOutputTokens, chunkModel);
            stageOutputs.add(response.content().summary());
            finalContent = response.content();

            long usedTokens = resolveTokensUsed(response, prompt);
            totalTokens += usedTokens;
            remainingTokens = updateRemainingTokens(remainingTokens, usedTokens);
            usageConsumer.accept(usedTokens);
        }

        while (stageOutputs.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int start = 0; start < stageOutputs.size(); start += safeGroupSize()) {
                int end = Math.min(start + safeGroupSize(), stageOutputs.size());
                List<String> group = stageOutputs.subList(start, end);

                String aggregationPrompt = SummaryPromptBuilder.buildAggregatePrompt(group, tone, length);
                int aggregationMaxTokens = resolveMaxOutputTokens(length, true, remainingTokens);

                SummaryLlmResponse aggregatedResponse = callWithRetry(
                        aggregationPrompt,
                        aggregationMaxTokens,
                        aggregateModel);

                nextLevel.add(aggregatedResponse.content().summary());
                finalContent = aggregatedResponse.content();

                long aggregationTokens = resolveTokensUsed(aggregatedResponse, aggregationPrompt);
                totalTokens += aggregationTokens;
                remainingTokens = updateRemainingTokens(remainingTokens, aggregationTokens);
                usageConsumer.accept(aggregationTokens);
            }
            stageOutputs = nextLevel;
        }

        if (finalContent == null) {
            throw new BaseException(
                    "Summary generation failed",
                    ExceptionCodes.DOCUMENT_PARSING_FAILED);
        }

        return new SummaryGenerationResult(
                finalContent,
                Math.toIntExact(totalTokens));
    }

    private SummaryLlmResponse callWithRetry(String prompt, int maxOutputTokens, String modelName) {
        RuntimeException last = null;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            int attemptMaxTokens = increaseMaxTokensForAttempt(maxOutputTokens, attempt);
            try {
                return llmService.generate(prompt, attemptMaxTokens, modelName);
            } catch (RuntimeException e) {

                if (e instanceof BaseException) {
                    throw e;
                }

                last = e;
                boolean retryable = isRetryable(e);
                log.warn("LLM call failed (model={}, attempt {}/{}, maxTokens={}): {}",
                        modelName, attempt, maxRetries + 1, attemptMaxTokens, e.getMessage());

                if (!retryable || attempt > maxRetries) {
                    break;
                }

                long delay = resolveBackoffDelay(attempt);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        throw last != null ? last : new RuntimeException("LLM call failed");
    }

    private boolean isRetryable(RuntimeException e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        String m = message.toLowerCase();
        return m.contains("429")
                || m.contains("rate limit")
                || m.contains("quota")
                || m.contains("timed out")
                || m.contains("503")
                || m.contains("502")
                || m.contains("500")
                || m.contains("incomplete_json_response_from_model")
                || m.contains("jsonmappingexception")
                || m.contains("jsonparseexception")
                || m.contains("unexpected end-of-input")
                || m.contains("was expecting closing quote");
    }

    private int increaseMaxTokensForAttempt(int baseMaxTokens, int attempt) {
        if (attempt <= 1) {
            return baseMaxTokens;
        }

        int boosted = baseMaxTokens + (attempt - 1) * 120;
        return Math.min(boosted, baseMaxTokens + 500);
    }

    private long resolveBackoffDelay(int attempt) {
        long exp = Math.min(maxDelayMs, baseDelayMs * (1L << Math.min(attempt - 1, 10)));
        long jitter = ThreadLocalRandom.current().nextLong(Math.max(100, exp / 2));
        return Math.min(maxDelayMs, exp + jitter);
    }

    private int resolveMaxOutputTokens(
            SummaryLength length,
            boolean finalPass,
            long remainingTokens) {


        int lengthBasedMax = switch (length) {
            case SHORT -> finalPass ? 320 : 260;
            case MEDIUM -> finalPass ? 700 : 420;
            case LONG -> finalPass ? 1500 : 700;
            case VERY_LONG -> finalPass ? 2400 : 900;
        };

        long safetyMargin = finalPass ? MIN_REQUIRED_TOKEN_DIFFERENCE : MIN_REQUIRED_TOKEN_DIFFERENCE / 2;
        long allowedOutputByQuota = remainingTokens - safetyMargin;
        if (allowedOutputByQuota < MIN_OUTPUT_TOKENS) {
            throw new BaseException(
                    "Quota exceeded",
                    ExceptionCodes.QUOTA_EXCEEDED);
        }

        int outputCap = (int) Math.min(lengthBasedMax, allowedOutputByQuota);
        if (outputCap < MIN_OUTPUT_TOKENS) {
            throw new BaseException(
                    "Quota exceeded",
                    ExceptionCodes.QUOTA_EXCEEDED);
        }

        return outputCap;
    }

    private long resolveTokensUsed(
            SummaryLlmResponse response,
            String prompt) {

        if (response.totalTokens() != null && response.totalTokens() > 0) {
            return response.totalTokens();
        }

        return tokenEstimator.estimateTokens(prompt)
                + tokenEstimator
                        .estimateTokens(response.content().summary());
    }

    private long updateRemainingTokens(
            long remaining,
            long used) {

        long updated = remaining - used;
        if (updated < 0) {
            throw new BaseException(
                    "Quota exceeded",
                    ExceptionCodes.QUOTA_EXCEEDED);
        }
        return updated;
    }

    private List<String> normalizeChunks(List<String> chunks) {
        if (chunks == null)
            return List.of();

        List<String> normalized = new ArrayList<>();
        for (String chunk : chunks) {
            String value = normalize(chunk);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private int safeGroupSize() {
        return Math.max(2, mergeGroupSize);
    }

    private String normalize(String text) {
        if (text == null)
            return "";
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
