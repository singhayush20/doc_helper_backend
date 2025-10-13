package com.ayushsingh.doc_helper.features.usage_monitoring.cron;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenQuotaRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.TokenUsageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaResetScheduler {

    private final UserTokenQuotaRepository quotaRepository;
    private final TokenUsageService tokenUsageService;

    private static final int BATCH_SIZE = 100;

    /**
     * Reset quotas for users whose reset date has passed
     * Runs every day at 12:01 AM in configured timezone
     */
    @Scheduled(cron = "0 1 0 * * ?", zone = "${monetization.billing" +
                                            ".billing-timezone}")
    public void resetExpiredQuotas() {
        log.info("=== Starting quota reset job ===");
        Instant startTime = Instant.now();

        Instant now = Instant.now();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        try {
            // Process in batches to avoid memory issues
            int page = 0;
            Page<UserTokenQuota> quotaPage;

            do {
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                quotaPage = quotaRepository.findQuotasToResetPaginated(now, pageable);

                if (quotaPage.hasContent()) {
                    log.info("Processing batch {}/{} - {} quotas",
                            page + 1, quotaPage.getTotalPages(), quotaPage.getNumberOfElements());

                    processBatch(quotaPage.getContent(), successCount, failureCount);
                }

                page++;
            } while (quotaPage.hasNext());

            Instant endTime = Instant.now();
            long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();

            log.info("=== Quota reset job completed ===");
            log.info("Duration: {} ms", durationMs);
            log.info("Successfully reset: {} quotas", successCount.get());
            log.info("Failed: {} quotas", failureCount.get());

        } catch (Exception e) {
            log.error("Quota reset job failed with unexpected error", e);
        }
    }

    /**
     * Process a batch of quotas
     * Each quota reset is in its own transaction
     */
    private void processBatch(List<UserTokenQuota> quotas,
            AtomicInteger successCount,
            AtomicInteger failureCount) {
        for (UserTokenQuota quota : quotas) {
            try {
                // Each reset in separate transaction
                tokenUsageService.resetQuota(quota);
                successCount.incrementAndGet();

                log.debug("Reset quota for userId: {}", quota.getUserId());
            } catch (Exception e) {
                failureCount.incrementAndGet();
                log.error("Failed to reset quota for userId: {}, error: {}",
                        quota.getUserId(), e.getMessage(), e);
            }
        }
    }
}
