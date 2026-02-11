package com.ayushsingh.doc_helper.features.doc_summary.service;

import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryTone;

import java.util.List;
import java.util.function.LongConsumer;

public interface SummaryGenerationService {
    SummaryGenerationResult generate(
            List<String> chunks,
            SummaryTone tone,
            SummaryLength length,
            long remainingTokens,
            LongConsumer tokenConsumer
    );
}
