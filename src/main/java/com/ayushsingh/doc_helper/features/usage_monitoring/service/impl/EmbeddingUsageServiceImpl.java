package com.ayushsingh.doc_helper.features.usage_monitoring.service.impl;

import com.ayushsingh.doc_helper.features.usage_monitoring.cofig.PricingConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.ChatOperationType;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenUsageRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.EmbeddingUsageService;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.TokenUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingUsageServiceImpl implements EmbeddingUsageService {

    private final TokenUsageService tokenUsageService;
    private final UserTokenUsageRepository usageRepository;
    private final PricingConfig pricingConfig;

    /**
     * Track embedding generation for document upload
     */
    @Transactional
    @Override
    public void recordEmbeddingUsage(Long userId, Long documentId,
            String embeddingModel, Integer numChunks, Long totalTokens) {

        log.debug(
                "Recording embedding usage: userId={}, documentId={}, chunks={}, tokens={}",
                userId, documentId, numChunks, totalTokens);

        // Check quota before processing (throws exception if exceeded)
        tokenUsageService.checkAndEnforceQuota(userId, totalTokens);

        // Calculate cost using pricing config
        BigDecimal cost = calculateEmbeddingCost(embeddingModel, totalTokens);

        // Create usage record
        UserTokenUsage usage = UserTokenUsage.builder()
                .userId(userId)
                .documentId(documentId)
                .promptTokens(totalTokens)
                .completionTokens(0L)
                .totalTokens(totalTokens)
                .modelName(embeddingModel)
                .documentChunks(numChunks)
                .operationType(ChatOperationType.EMBEDDING_GENERATION)
                .estimatedCost(cost)
                .durationMs(0L)
                .createdAt(Instant.now())
                .build();

        usageRepository.save(usage);

        // Update quota
        tokenUsageService.updateUserQuota(userId, totalTokens);

        log.info(
                "Embedding usage recorded: userId={}, documentId={}, chunks={}, tokens={}, cost={}",
                userId, documentId, numChunks, totalTokens, cost);
    }

    /**
     * Calculate embedding cost based on model and tokens
     */
    private BigDecimal calculateEmbeddingCost(String modelName, Long tokens) {
        // For embeddings, only input cost applies (no output)
        BigDecimal costPer1k = pricingConfig.getInputCost(modelName);

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
        return textChunks.stream().mapToLong(this::estimateTokensForText).sum();
    }
}
