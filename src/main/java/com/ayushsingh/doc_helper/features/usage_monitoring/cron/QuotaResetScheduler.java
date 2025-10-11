package com.ayushsingh.doc_helper.features.usage_monitoring.cron;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenQuotaRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.TokenUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaResetScheduler {

    private final UserTokenQuotaRepository quotaRepository;
    private final TokenUsageService tokenUsageService;

    /**
     * Reset quotas for users whose reset date has passed
     * Runs every day at 12:01 AM IST
     */
    @Scheduled(cron = "0 1 0 * * ?", zone = "${monetization.billing.timezone}")
    @Transactional
    public void resetExpiredQuotas() {
        log.info("Starting quota reset job");

        Instant now = Instant.now();
        List<UserTokenQuota> quotasToReset = quotaRepository.findQuotasToReset(
                now);

        log.info("Found {} quotas to reset", quotasToReset.size());

        for (UserTokenQuota quota : quotasToReset) {
            try {
                tokenUsageService.resetQuota(quota);
                log.info("Reset quota for userId: {}", quota.getUserId());
            } catch (Exception e) {
                log.error("Failed to reset quota for userId: {}",
                        quota.getUserId(), e);
            }
        }

        log.info("Quota reset job completed. Reset {} quotas",
                quotasToReset.size());
    }
}
