package com.ayushsingh.doc_helper.features.usage_monitoring.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;

public interface QuotaManagementService {
    void checkAndEnforceQuota(Long userId, Long tokensToUse);

    void updateUserQuota(Long userId, Long tokensUsed);

    UserTokenQuota getCurrentUserQuota(Long userId);

    Long getCurrentMonthUsage(Long userId);

    UserTokenQuota createDefaultQuota(Long userId);

    void resetQuota(UserTokenQuota quota);

    void updateUserTier(Long userId, String newTier, Long newLimit);

    Page<UserTokenQuota> findQuotasToResetPaginated(Instant instant, Pageable pageable);
}
