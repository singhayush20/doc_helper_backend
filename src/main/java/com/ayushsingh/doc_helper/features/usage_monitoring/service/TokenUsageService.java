package com.ayushsingh.doc_helper.features.usage_monitoring.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ayushsingh.doc_helper.features.usage_monitoring.dto.DailyUsageSummary;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.QuotaInfoResponse;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.TokenUsageDto;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.UsageBreakdown;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;

public interface TokenUsageService {

        void recordTokenUsage(TokenUsageDto usageDTO);

        void checkAndEnforceQuota(Long userId, Long tokensToUse);

        void updateUserQuota(Long userId, Long tokensUsed);

        UserTokenQuota getOrCreateQuota(Long userId);

        void resetQuota(UserTokenQuota quota);

        Long getCurrentMonthUsage(Long userId);

        QuotaInfoResponse getUserQuotaInfo(Long userId);

        Page<UserTokenUsage> getUserUsageHistory(Long userId, Pageable pageable);

        Page<UserTokenUsage> getDocumentUsageHistory(
                        Long userId, Long documentId, Pageable pageable);

        List<DailyUsageSummary> getDailyUsageSummary(
                        Long userId, Instant startDate, Instant endDate);

        BigDecimal getTotalCost(Long userId, Instant startDate);

        void updateUserTier(Long userId, String newTier, Long newLimit);

        UsageBreakdown getUsageBreakdown(Long userId);

        UsageBreakdown getUsageBreakdownByDateRange(
                        Long userId,
                        Instant startDate,
                        Instant endDate);

}
