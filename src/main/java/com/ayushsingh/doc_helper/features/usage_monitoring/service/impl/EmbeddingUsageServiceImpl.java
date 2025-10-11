package com.ayushsingh.doc_helper.features.usage_monitoring.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenUsageRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.EmbeddingUsageService;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.TokenUsageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingUsageServiceImpl implements EmbeddingUsageService {

        private final TokenUsageService tokenUsageService;
        private final UserTokenUsageRepository usageRepository;

        // Embedding model pricing per 1K tokens
        private static final Map<String, BigDecimal> EMBEDDING_COSTS = Map.of(
                        "text-embedding-ada-002", BigDecimal.valueOf(0.0001), // OpenAI
                        "text-embedding-3-small", BigDecimal.valueOf(0.00002), // OpenAI
                        "text-embedding-3-large", BigDecimal.valueOf(0.00013), // OpenAI
                        "all-MiniLM-L6-v2", BigDecimal.valueOf(0.0), // Local/Free
                        "nomic-embed-text", BigDecimal.valueOf(0.0), // Ollama - Free
                        "mxbai-embed-large", BigDecimal.valueOf(0.0), // Ollama - Free
                        "default", BigDecimal.valueOf(0.0001));

        /**
         * Track embedding generation for document upload
         */
        @Transactional
        @Override
        public void recordEmbeddingUsage(
                        Long userId,
                        Long documentId,
                        String embeddingModel,
                        Integer numChunks,
                        Long totalTokens) {

                log.debug("Recording embedding usage: userId={}, documentId={}, chunks={}, tokens={}",
                                userId, documentId, numChunks, totalTokens);

                // Check quota before processing
                tokenUsageService.checkAndEnforceQuota(userId, totalTokens);

                // Calculate cost
                BigDecimal cost = calculateEmbeddingCost(embeddingModel, totalTokens);

                // Create usage record
                UserTokenUsage usage = UserTokenUsage.builder()
                                .userId(userId)
                                .documentId(documentId)
                                .timestamp(Instant.now())
                                .promptTokens(totalTokens) // For embeddings, all tokens are "input"
                                .completionTokens(0L) // No completion for embeddings
                                .totalTokens(totalTokens)
                                .modelName(embeddingModel)
                                .operationType("embedding")
                                .estimatedCost(cost)
                                .createdAt(Instant.now())
                                .build();

                usageRepository.save(usage);

                // Update quota
                tokenUsageService.updateUserQuota(userId, totalTokens);

                log.info("Embedding usage recorded: userId={}, documentId={}, chunks={}, tokens={}, cost={}",
                                userId, documentId, numChunks, totalTokens, cost);
        }

        /**
         * Calculate embedding cost based on model and tokens
         */
        private BigDecimal calculateEmbeddingCost(String modelName, Long tokens) {
                String modelKey = modelName != null && EMBEDDING_COSTS.containsKey(modelName)
                                ? modelName
                                : "default";

                BigDecimal costPer1k = EMBEDDING_COSTS.get(modelKey);

                return BigDecimal.valueOf(tokens)
                                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                                .multiply(costPer1k)
                                .setScale(6, RoundingMode.HALF_UP);
        }

        /**
         * Estimate tokens for text before embedding
         * Rough estimate: 1 token ≈ 4 characters for English text
         */
        @Override
        public Long estimateTokensForText(String text) {
                if (text == null || text.isEmpty()) {
                        return 0L;
                }
                // Simple estimation: divide by 4
                return (long) Math.ceil(text.length() / 4.0);
        }

        /**
         * Estimate total tokens for multiple chunks
         */
        @Override
        public Long estimateTotalTokens(java.util.List<String> textChunks) {
                return textChunks.stream()
                                .mapToLong(this::estimateTokensForText)
                                .sum();
        }
}
