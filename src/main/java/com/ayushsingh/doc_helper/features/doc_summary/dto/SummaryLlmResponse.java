package com.ayushsingh.doc_helper.features.doc_summary.dto;

public record SummaryLlmResponse(
        String content,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}
