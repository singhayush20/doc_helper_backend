package com.ayushsingh.doc_helper.features.usage_monitoring.cron;

import java.time.Instant;
import java.util.List;

import com.ayushsingh.doc_helper.features.user_plan.entity.SubscriptionStatus;
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

    @Scheduled(cron = "0 1 0 * * ?", zone = "${monetization.billing.billing-timezone}")
    public void processBillingTransitions() {

        Instant now = Instant.now();
        log.info("=== Billing transition job started ===");

        handleCheckoutExpiries(now);
        handleScheduledDowngrades(now);
        handleQuotaResets(now);

        log.info("=== Billing transition job completed ===");
    }

    /* ---------------- Checkout timeout ---------------- */

    private void handleCheckoutExpiries(Instant now) {

        List<Subscription> subs =
                subscriptionRepository.findCheckoutExpiredSubscriptions(now);

        for (Subscription s : subs) {
            try {
                log.warn("Checkout expired: {}", s.getId());

                s.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(s);

                subscriptionFallbackService.applyFreePlan(
                        s.getUser().getId());

            } catch (Exception ex) {
                log.error("Checkout expiry failed: {}", s.getId(), ex);
            }
        }
    }

    /* ---------------- Paid period ended ---------------- */

    private void handleScheduledDowngrades(Instant now) {

        List<Subscription> subs =
                subscriptionRepository.findSubscriptionsReadyForFallback(now);

        for (Subscription s : subs) {
            try {
                log.info("Applying fallback for subscription {}", s.getId());

                subscriptionFallbackService.applyFreePlan(
                        s.getUser().getId());

                s.setStatus(SubscriptionStatus.CANCELED);
                subscriptionRepository.save(s);

            } catch (Exception ex) {
                log.error("Fallback failed for {}", s.getId(), ex);
            }
        }
    }

    /* ---------------- Quota reset ---------------- */

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
                    log.error("Quota reset failed for {}",
                            quota.getUserId(), ex);
                }
            }
            page++;
        } while (quotaPage.hasNext());
    }
}
