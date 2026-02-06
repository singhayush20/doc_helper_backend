package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryTone;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryGenerationResult;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryGenerationServiceImpl implements SummaryGenerationService {

    private static final int MAX_RETRIES = 2;
    private static final Duration LLM_TIMEOUT = Duration.ofSeconds(45);

    private final SummaryChunker chunker;
    private final SummaryPromptBuilder promptBuilder;
    private final SummaryLlmService llmService;
    private final SummaryTokenEstimator tokenEstimator;

    @Override
    public SummaryGenerationResult generate(
            String documentText,
            SummaryTone tone,
            SummaryLength length
    ) {
        String normalized = normalize(documentText);
        List<String> chunks = chunker.split(normalized, length);

        List<String> chunkSummaries = new ArrayList<>();
        int totalTokens = 0;

        for (String chunk : chunks) {
            String prompt = promptBuilder.buildChunkPrompt(chunk, tone, length);
            String summary = callWithRetry(prompt);
            chunkSummaries.add(summary);
            totalTokens += tokenEstimator.estimateTokens(chunk) +
                    tokenEstimator.estimateTokens(summary);
        }

        List<String> current = chunkSummaries;
        while (current.size() > 1) {
            current = aggregateLevel(current, tone, length, false);
        }

        String finalPrompt = promptBuilder.buildAggregatePrompt(
                current, tone, length, true);
        String finalSummary = callWithRetry(finalPrompt);
        totalTokens += tokenEstimator.estimateTokens(finalSummary);

        return new SummaryGenerationResult(finalSummary, totalTokens);
    }

    @Override
    public long estimateTokens(String documentText, SummaryLength length) {
        String normalized = normalize(documentText);
        List<String> chunks = chunker.split(normalized, length);
        long tokens = 0;
        for (String chunk : chunks) {
            tokens += tokenEstimator.estimateTokens(chunk);
        }
        long overhead = Math.max(1, tokens / 10);
        return tokens + overhead;
    }

    private List<String> aggregateLevel(
            List<String> summaries,
            SummaryTone tone,
            SummaryLength length,
            boolean finalPass
    ) {
        int groupSize = switch (length) {
            case SHORT -> 6;
            case MEDIUM -> 4;
            case LONG -> 3;
        };

        List<String> next = new ArrayList<>();
        for (int i = 0; i < summaries.size(); i += groupSize) {
            List<String> group =
                    summaries.subList(i, Math.min(i + groupSize, summaries.size()));
            String prompt = promptBuilder.buildAggregatePrompt(group, tone, length, finalPass);
            next.add(callWithRetry(prompt));
        }
        return next;
    }

    private String callWithRetry(String prompt) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            try {
                return CompletableFuture.supplyAsync(() -> llmService.generate(prompt))
                        .orTimeout(LLM_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                        .join();
            } catch (RuntimeException e) {
                last = e;
                log.warn("LLM call failed (attempt {}/{}): {}",
                        attempt, MAX_RETRIES + 1, e.getMessage());
            }
        }
        throw last != null ? last : new RuntimeException("LLM call failed");
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }
}
