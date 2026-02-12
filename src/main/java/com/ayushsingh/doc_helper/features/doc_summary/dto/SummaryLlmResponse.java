package com.ayushsingh.doc_helper.features.doc_summary.dto;

public record SummaryLlmResponse(
                StructuredSummaryDto content,
                Integer promptTokens,
                Integer completionTokens,
                Integer totalTokens) {
}
