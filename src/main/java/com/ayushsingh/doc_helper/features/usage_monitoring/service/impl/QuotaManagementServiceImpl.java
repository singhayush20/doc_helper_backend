package com.ayushsingh.doc_helper.features.usage_monitoring.service.impl;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.usage_monitoring.cofig.BillingConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.cofig.PlanConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.AccountTier;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenQuotaRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaManagementServiceImpl implements QuotaManagementService {

    private final UserTokenQuotaRepository quotaRepository;
    private final BillingConfig billingConfig;
    private final PlanConfig planConfig;

    @Transactional(readOnly = true)
    @Override
    public void checkAndEnforceQuota(Long userId, Long tokensToUse) {
        UserTokenQuota quota = getCurrentUserQuota(userId);

        if (Boolean.FALSE.equals(quota.getIsActive())) {
            log.warn("Inactive user attempted to use tokens: userId={}", userId);
            throw new BaseException(
                    "User account is inactive",
                    ExceptionCodes.USER_QUOTA_INACTIVE);
        }

        long remaining = quota.getMonthlyLimit() - quota.getCurrentMonthlyUsage();
        if (remaining < tokensToUse) {
            log.warn(
                    "Insufficient quota to start request: userId={}, remaining={}, required={}",
                    userId, remaining, tokensToUse);
            throw new BaseException(
                    "Insufficient token quota to start this operation.",
                    ExceptionCodes.QUOTA_EXCEEDED);
        }

        log.debug("Soft quota pre-check passed for userId: {}, remaining: {}, required: {}",
                userId, remaining, tokensToUse);
    }

    @Transactional
    @Override
    public void updateUserQuota(Long userId, Long tokensUsed) {
        log.warn("updateUserQuota called for userId={} with tokensUsed={}. " +
                "This should only be used for admin/manual adjustments.",
                userId, tokensUsed);
        quotaRepository.incrementUsage(userId, tokensUsed);
    }

    @Transactional(readOnly = true)
    @Override
    public UserTokenQuota getCurrentUserQuota(Long userId) {
        return quotaRepository.findByUserId(userId)
                .orElseThrow(() -> new BaseException(
                        "No quota info found for user: " + userId,
                        ExceptionCodes.QUOTA_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    @Override
    public Long getCurrentMonthUsage(Long userId) {
        UserTokenQuota quota = getCurrentUserQuota(userId);
        return quota.getCurrentMonthlyUsage();
    }

    @Transactional
    @Override
    public UserTokenQuota createDefaultQuota(Long userId) {
        log.debug("Creating default quota for userId: {}", userId);

        Instant resetDate = getNextMonthStart();

        PlanConfig.PlanLimits freeLimits = planConfig.getLimits(AccountTier.FREE);
        Long monthlyLimit = freeLimits.getMonthlyTokenLimit();

        UserTokenQuota quota = UserTokenQuota.builder()
                .userId(userId)
                .monthlyLimit(monthlyLimit)
                .currentMonthlyUsage(0L)
                .resetDate(resetDate)
                .tier(AccountTier.FREE)
                .isActive(true)
                .build();

        return quotaRepository.save(quota);
    }

    @Transactional
    @Override
    public void resetQuota(UserTokenQuota quota) {
        Long userId = quota.getUserId();
        log.debug("Resetting quota for userId: {}", userId);

        Instant now = Instant.now();
        Instant newResetDate = getNextMonthStart();

        int updated = quotaRepository.resetQuota(userId, now, newResetDate);
        if (updated == 0) {
            log.debug("Quota already reset or not due for userId={}", userId);
        } else {
            log.info("Quota reset for userId={}", userId);
        }
    }

    @Transactional
    @Override
    public void updateUserTier(Long userId, String newTier, Long newLimit) {
        log.error("Account upgradation not implemented!");

        throw new BaseException(
                "Account upgradation is not supported at the moment!",
                ExceptionCodes.ACCOUNT_UPGRADATION_FAILURE);
    }

    private Instant getNextMonthStart() {
        ZoneId zoneId = ZoneId.of(billingConfig.getBillingTimezone());
        YearMonth nextMonth = YearMonth.now(zoneId).plusMonths(1);
        return nextMonth
                .atDay(1)
                .atStartOfDay(zoneId)
                .toInstant();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserTokenQuota> findQuotasToResetPaginated(Instant instant, Pageable pageable) {
        return quotaRepository.findQuotasToResetPaginated(instant, pageable);
    }
}