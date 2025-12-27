package com.ayushsingh.doc_helper.features.usage_monitoring.service;

import com.ayushsingh.doc_helper.features.usage_monitoring.dto.TokenUsageDto;

public interface UsageRecordingService {
    void recordTokenUsage(TokenUsageDto usageDto);
}
