package com.ayushsingh.doc_helper.features.usage_monitoring.cron;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import com.ayushsingh.doc_helper.features.user_plan.entity.Subscription;
import com.ayushsingh.doc_helper.features.user_plan.repository.SubscriptionRepository;
import com.ayushsingh.doc_helper.features.user_plan.service.SubscriptionFallbackService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionAndQuotaScheduler {

    private final QuotaManagementService quotaManagementService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionFallbackService subscriptionFallbackService;

    private static final int BATCH_SIZE = 100;

    /**
     * Runs every day at 12:01 AM (billing timezone)
     */
    @Scheduled(cron = "0 1 0 * * ?", zone = "${monetization.billing.billing-timezone}")
    public void processBillingTransitions() {

        Instant now = Instant.now();
        log.info("=== Starting billing transition job ===");

        handleScheduledDowngrades(now);
        handleQuotaResets(now);

        log.info("=== Billing transition job completed ===");
    }

    /**
     * Apply FREE fallback for cancelled subscriptions
     * whose billing period has ended.
     */
    private void handleScheduledDowngrades(Instant now) {

        List<Subscription> subscriptions = subscriptionRepository
                .findCancelledSubscriptionsReadyForFallback(now);

        for (Subscription subscription : subscriptions) {
            try {
                log.info(
                        "Applying scheduled FREE fallback for userId={}, subscriptionId={}",
                        subscription.getUser().getId(),
                        subscription.getId());

                subscriptionFallbackService.applyFreePlan(
                        subscription.getUser().getId());

            } catch (Exception ex) {
                log.error(
                        "Failed scheduled FREE fallback for userId={}",
                        subscription.getUser().getId(),
                        ex);
            }
        }
    }

    /**
     * Reset quota for users whose quota reset date passed
     * (active subscriptions only).
     */
    private void handleQuotaResets(Instant now) {

        int page = 0;
        Page<UserTokenQuota> quotaPage;

        do {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            quotaPage = quotaManagementService
                    .findQuotasToResetPaginated(now, pageable);

            for (UserTokenQuota quota : quotaPage.getContent()) {
                try {
                    quotaManagementService.resetQuotaForNewBillingCycle(
                            quota.getUserId());
                } catch (Exception ex) {
                    log.error(
                            "Failed quota reset for userId={}",
                            quota.getUserId(),
                            ex);
                }
            }

            page++;

        } while (quotaPage.hasNext());
    }
}
