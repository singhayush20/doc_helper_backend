package com.ayushsingh.doc_helper.features.doc_summary.service;

import com.ayushsingh.doc_helper.features.doc_summary.dto.StructuredSummaryDto;

public record SummaryGenerationResult(StructuredSummaryDto content, int tokensUsed) {
}
