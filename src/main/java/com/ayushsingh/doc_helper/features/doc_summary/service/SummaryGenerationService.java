package com.ayushsingh.doc_helper.features.doc_summary.service;

import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryTone;

public interface SummaryGenerationService {
    SummaryGenerationResult generate(String documentText, SummaryTone tone, SummaryLength length);

    long estimateTokens(String documentText, SummaryLength length);
}
