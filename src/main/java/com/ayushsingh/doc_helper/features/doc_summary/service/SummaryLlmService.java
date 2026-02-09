package com.ayushsingh.doc_helper.features.doc_summary.service;

import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryLlmResponse;

public interface SummaryLlmService {
    public SummaryLlmResponse generate(String prompt, Integer maxTokens);
}
