package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentTokenEstimationService;

@Component
@Slf4j
public class DocumentTokenEstimationServiceImpl implements DocumentTokenEstimationService {

    private final JTokkitTokenCountEstimator estimator = new JTokkitTokenCountEstimator();

    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            return Math.toIntExact(estimator.estimate(text));
        } catch (Exception e) {
            log.warn("Token estimation failed, using character fallback");
            return text.length() / 4;
        }
    }
}
