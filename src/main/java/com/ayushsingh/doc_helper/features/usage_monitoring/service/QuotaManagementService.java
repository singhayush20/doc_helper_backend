package com.ayushsingh.doc_helper.features.usage_monitoring.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;

public interface QuotaManagementService {

    // ---------- Runtime enforcement ----------
    void checkAndEnforceQuota(Long userId, Long tokensToUse);

    void incrementUsage(Long userId, Long tokensUsed);

    UserTokenQuota getQuota(Long userId);

    Long getCurrentMonthlyUsage(Long userId);

    // ---------- Subscription driven ----------
    void applySubscriptionQuota(Long userId, Long monthlyTokenLimit);

    void resetQuotaForNewBillingCycle(Long userId);

    void deactivateQuota(Long userId);

    // ---------- System ----------
    UserTokenQuota createInitialQuota(Long userId);

    Page<UserTokenQuota> findQuotasToResetPaginated(Instant now, Pageable pageable);
}
