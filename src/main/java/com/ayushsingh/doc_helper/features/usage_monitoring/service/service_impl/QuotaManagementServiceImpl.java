package com.ayushsingh.doc_helper.features.usage_monitoring.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
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

    private static final long DEFAULT_MONTHLY_TOKEN_LIMIT = 10000;
    private final UserTokenQuotaRepository quotaRepository;

    @Override
    @Transactional(readOnly = true)
    public void checkAndEnforceQuota(Long userId, Long tokensToUse) {
        UserTokenQuota quota = getQuota(userId);

        if (Boolean.FALSE.equals(quota.getIsActive())) {
            throw new BaseException(
                    "User quota is inactive",
                    ExceptionCodes.USER_QUOTA_INACTIVE);
        }

        long remaining = quota.getMonthlyLimit() - quota.getCurrentMonthlyUsage();
        if (remaining < tokensToUse) {
            throw new BaseException(
                    "Insufficient token quota",
                    ExceptionCodes.QUOTA_EXCEEDED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserTokenQuota getQuota(Long userId) {
        return quotaRepository.findByUserId(userId)
                .orElseThrow(() -> new BaseException(
                        "Quota not found for user: " + userId,
                        ExceptionCodes.QUOTA_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCurrentMonthlyUsage(Long userId) {
        return getQuota(userId).getCurrentMonthlyUsage();
    }

    @Override
    @Transactional
    public void applySubscriptionQuota(Long userId, Long monthlyTokenLimit) {

        UserTokenQuota quota = getQuota(userId);

        log.info("Applying subscription quota userId={}, limit={}",
                userId, monthlyTokenLimit);

        quota.setMonthlyLimit(monthlyTokenLimit);
        quota.setCurrentMonthlyUsage(0L);
        quota.setResetDate(oneMonthFromNow());
        quota.setIsActive(true);

        quotaRepository.save(quota);
    }

    @Override
    @Transactional
    public void resetQuotaForNewBillingCycle(Long userId) {

        UserTokenQuota quota = getQuota(userId);

        log.info("Resetting quota for new billing cycle userId={}", userId);

        quota.setCurrentMonthlyUsage(0L);
        quota.setResetDate(oneMonthFromNow());

        quotaRepository.save(quota);
    }

    @Override
    @Transactional
    public void deactivateQuota(Long userId) {

        UserTokenQuota quota = getQuota(userId);

        quota.setIsActive(false);
        quotaRepository.save(quota);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserTokenQuota> findQuotasToResetPaginated(
            Instant now, Pageable pageable) {

        return quotaRepository.findQuotasToResetPaginated(now, pageable);
    }

    @Override
    @Transactional
    public void applyFreeQuota(Long userId) {
        UserTokenQuota quota = getQuota(userId);

        quota.setMonthlyLimit(DEFAULT_MONTHLY_TOKEN_LIMIT);
        quota.setCurrentMonthlyUsage(0L);
        quota.setResetDate(oneMonthFromNow());
        quota.setIsActive(true);

        quotaRepository.save(quota);
    }

    private Instant oneMonthFromNow() {
        return ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .plusMonths(1)
                .toInstant();
    }
}
