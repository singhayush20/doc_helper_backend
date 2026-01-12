package com.ayushsingh.doc_helper.features.user_plan.cron;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ayushsingh.doc_helper.features.payments.entity.ProviderSubscriptionStatus;
import com.ayushsingh.doc_helper.features.payments.service.PaymentProviderClient;
import com.ayushsingh.doc_helper.features.user_plan.entity.Subscription;
import com.ayushsingh.doc_helper.features.user_plan.entity.SubscriptionStatus;
import com.ayushsingh.doc_helper.features.user_plan.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionStatusCron {

    private static final Duration GRACE_WINDOW = Duration.ofMinutes(5);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentProviderClient paymentProviderClient;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void reconcileAndExpireIncompleteSubscriptions() {

        Instant now = Instant.now();

        List<Subscription> incompletes = subscriptionRepository.findByStatus(
                SubscriptionStatus.INCOMPLETE);

        for (Subscription sub : incompletes) {

            Instant effectiveExpiry = sub.getCheckoutExpiresAt().plus(GRACE_WINDOW);

            if (now.isBefore(effectiveExpiry)) {
                continue;
            }

            ProviderSubscriptionStatus providerStatus = paymentProviderClient.fetchSubscriptionStatus(
                    sub.getProviderSubscriptionId());

            switch (providerStatus) {

                case ACTIVE, AUTHENTICATED -> {
                    log.info("Reconciling {} → ACTIVE", sub.getId());
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                }

                case PENDING -> {
                    log.info("Reconciling {} → PAST_DUE", sub.getId());
                    sub.setStatus(SubscriptionStatus.PAST_DUE);
                }

                case HALTED -> {
                    log.info("Reconciling {} → HALTED", sub.getId());
                    sub.setStatus(SubscriptionStatus.HALTED);
                }

                case CANCELLED, COMPLETED, EXPIRED -> {
                    log.info("Reconciling {} → EXPIRED", sub.getId());
                    sub.setStatus(SubscriptionStatus.EXPIRED);
                }

                case CREATED, UNKNOWN -> {
                    log.info("Expiring stale checkout {}", sub.getId());
                    sub.setStatus(SubscriptionStatus.EXPIRED);
                }
            }
        }
    }
}
