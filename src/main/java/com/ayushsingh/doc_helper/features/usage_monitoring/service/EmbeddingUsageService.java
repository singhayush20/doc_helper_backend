package com.ayushsingh.doc_helper.features.usage_monitoring.service;

public interface EmbeddingUsageService {
    void recordEmbeddingUsage(
            Long userId,
            Long documentId,
            String embeddingModel,
            Integer numChunks,
            Long totalTokens);

    Long estimateTokensForText(String text);

    Long estimateTotalTokens(java.util.List<String> textChunks);
}
