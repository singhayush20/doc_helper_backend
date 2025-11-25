package com.ayushsingh.doc_helper.features.usage_monitoring.service;

import com.ayushsingh.doc_helper.features.usage_monitoring.dto.DailyUsageSummaryResponse;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.QuotaInfoResponse;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.UsageBreakdown;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;

public interface UsageReportingService {
    QuotaInfoResponse getUserQuotaInfo(Long userId);

    Page<UserTokenUsage> getUserUsageHistory(Long userId, Pageable pageable);

    Page<UserTokenUsage> getDocumentUsageHistory(Long userId, Long documentId, Pageable pageable);

    DailyUsageSummaryResponse getDailyUsageSummaryForUser(Long userId, int days);

    BigDecimal getTotalCost(Long userId, Instant startDate);

    UsageBreakdown getUsageBreakdown(Long userId);

    UsageBreakdown getUsageBreakdownByDateRange(Long userId, Instant startDate, Instant endDate);
}
