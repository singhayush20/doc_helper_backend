package com.ayushsingh.doc_helper.features.usage_monitoring.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.ayushsingh.doc_helper.features.usage_monitoring.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;

public interface TokenUsageService {

        void recordTokenUsage(TokenUsageDto usageDTO);

        UserTokenQuota createDefaultQuota(Long userId);

        void checkAndEnforceQuota(Long userId, Long tokensToUse);

        void updateUserQuota(Long userId, Long tokensUsed);

        UserTokenQuota getCurrentUserQuota(Long userId);

        void resetQuota(UserTokenQuota quota);

        Long getCurrentMonthUsage(Long userId);

        QuotaInfoResponse getUserQuotaInfo(Long userId);

        Page<UserTokenUsage> getUserUsageHistory(Pageable pageable);

        Page<UserTokenUsage> getDocumentUsageHistory(
                        Long documentId, Pageable pageable);

        DailyUsageSummaryResponse getDailyUsageSummaryForDays(int days);

        BigDecimal getTotalCost(Long userId, Instant startDate);

        void updateUserTier(Long userId, String newTier, Long newLimit);

        UsageBreakdown getUsageBreakdown(Long userId);

        UsageBreakdown getUsageBreakdownByDateRange(
                        Long userId,
                        Instant startDate,
                        Instant endDate);

}
