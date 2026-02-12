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

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryGenerationServiceImpl implements SummaryGenerationService {

    private static final int MAX_RETRIES = 2;
    private static final int MIN_REQUIRED_TOKEN_DIFFERENCE = 100;

    private final SummaryLlmService llmService;
    private final DocumentTokenEstimationService tokenEstimator;

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

        List<String> chunkSummaries = new ArrayList<>();
        long totalTokens = 0;

        for (String chunk : normalizedChunks) {

            String prompt = SummaryPromptBuilder.buildChunkPrompt(chunk, tone, length);

            int maxOutputTokens = resolveMaxOutputTokens(prompt, length, false, remainingTokens);

            SummaryLlmResponse response = callWithRetry(prompt, maxOutputTokens);

            chunkSummaries.add(response.content().summary());

            long usedTokens = resolveTokensUsed(response, prompt);
            totalTokens += usedTokens;
            remainingTokens = updateRemainingTokens(remainingTokens, usedTokens);
            usageConsumer.accept(usedTokens);
        }

        String aggregationPrompt = SummaryPromptBuilder.buildAggregatePrompt(
                chunkSummaries, tone, length);

        int aggregationMaxTokens = resolveMaxOutputTokens(
                aggregationPrompt, length, true, remainingTokens);

        SummaryLlmResponse aggregatedResponse = callWithRetry(aggregationPrompt, aggregationMaxTokens);

        long aggregationTokens = resolveTokensUsed(aggregatedResponse, aggregationPrompt);

        totalTokens += aggregationTokens;
        usageConsumer.accept(aggregationTokens);

        return new SummaryGenerationResult(
                aggregatedResponse.content(),
                Math.toIntExact(totalTokens));
    }

    private SummaryLlmResponse callWithRetry(String prompt, int maxOutputTokens) {
        RuntimeException last = null;

        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            try {
                return llmService.generate(prompt, maxOutputTokens);
            } catch (RuntimeException e) {

                if (e instanceof BaseException) {
                    throw e; // never retry business errors
                }

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

    private int resolveMaxOutputTokens(
            String prompt,
            SummaryLength length,
            boolean finalPass,
            long remainingTokens) {

        int promptTokens = tokenEstimator.estimateTokens(prompt);

        var lengthBasedMax = switch (length) {
            case SHORT -> 300;
            case MEDIUM -> 500;
            case LONG -> 900;
            case VERY_LONG -> 1200;
        };

        long allowed = remainingTokens - promptTokens - MIN_REQUIRED_TOKEN_DIFFERENCE;
        if (allowed <= 0) {
            throw new BaseException(
                    "Quota exceeded",
                    ExceptionCodes.QUOTA_EXCEEDED);
        }

        return (int) Math.min(lengthBasedMax, allowed);
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
