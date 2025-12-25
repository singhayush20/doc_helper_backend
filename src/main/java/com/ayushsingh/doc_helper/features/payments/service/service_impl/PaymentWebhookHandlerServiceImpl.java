package com.ayushsingh.doc_helper.features.payments.service.service_impl;

import com.ayushsingh.doc_helper.features.payments.entity.PaymentProviderEventLog;
import com.ayushsingh.doc_helper.features.payments.repository.PaymentProviderEventLogRepository;
import com.ayushsingh.doc_helper.features.payments.service.PaymentProviderClient;
import com.ayushsingh.doc_helper.features.payments.service.PaymentWebhookHandlerService;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;
import com.ayushsingh.doc_helper.features.user_plan.entity.Subscription;
import com.ayushsingh.doc_helper.features.user_plan.entity.SubscriptionStatus;
import com.ayushsingh.doc_helper.features.user_plan.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookHandlerServiceImpl implements PaymentWebhookHandlerService {

    private final PaymentProviderClient paymentProviderClient;
    private final PaymentProviderEventLogRepository eventLogRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final QuotaManagementService quotaManagementService;

    @Override
    @Transactional
    public void handleProviderEvent(String rawPayload, String signatureHeader) {
        // 1. Verify signature
        paymentProviderClient.validateWebhookSignature(rawPayload, signatureHeader);

        // 2. Extract event id and type
        String eventId = paymentProviderClient.extractEventId(rawPayload);
        String eventType = paymentProviderClient.extractEventType(rawPayload);

        // 3. Idempotency
        Optional<PaymentProviderEventLog> existing = eventLogRepository.findByProviderEventId(eventId);
        if (existing.isPresent() && existing.get().isProcessed()) {
            log.info("Ignoring duplicate event: {}", eventId);
            return;
        }

        PaymentProviderEventLog eventLog = existing.orElseGet(() -> PaymentProviderEventLog.builder()
                .providerEventId(eventId)
                .eventType(eventType)
                .receivedAt(Instant.now())
                .processed(false)
                .rawPayload(rawPayload)
                .build());

        // 4. Process event
        processEvent(eventType, rawPayload);

        eventLog.setProcessed(true);
        eventLog.setProcessedAt(Instant.now());
        eventLogRepository.save(eventLog);
    }

    private void processEvent(String eventType, String rawPayload) {
        // This is intentionally generic; you will map Razorpay event names here
        // Example event types (adjust to actual Razorpay values):
        // "subscription.activated", "subscription.charged", "subscription.cancelled",
        // "subscription.halted"

        String providerSubId = paymentProviderClient.extractSubscriptionId(rawPayload);
        if (providerSubId == null) {
            log.warn("No subscription id in event: type={}, payload={}", eventType, rawPayload);
            return;
        }

        Subscription subscription = subscriptionRepository.findByProviderSubscriptionId(providerSubId)
                .orElse(null);
        if (subscription == null) {
            log.warn("No local subscription for providerSubId={}", providerSubId);
            return;
        }

        switch (eventType) {
            case "subscription.activated" -> handleActivated(subscription);
            case "subscription.charged" -> handleCharged(subscription);
            case "subscription.cancelled", "subscription.completed" -> handleCancelled(subscription);
            case "subscription.halted" -> handleHalted(subscription);
            default -> log.info("Unhandled event type: {}", eventType);
        }
    }

    private void handleActivated(Subscription subscription) {
        log.info("Handling subscription activation for id={}", subscription.getId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        // TODO: optionally set currentPeriodStart/End using provider data
        subscriptionRepository.save(subscription);

        // Upgrade tier based on BillingProduct.tier + PlanConfig for token limit
        BillingPrice price = subscription.getBillingPrice();
        AccountTier tier = price.getProduct().getTier();
        quotaManagementService.updateUserTier(
                subscription.getUser().getId(),
                tier.name(),
                null // let QuotaManagementService derive limit from PlanConfig
        );
    }

    private void handleCharged(Subscription subscription) {
        log.info("Handling subscription charged for id={}", subscription.getId());
        // Optionally: align resetDate with provider's period; or rely on your quota
        // scheduler
        // If you want subscription-driven reset:
        // quotaManagementService.resetQuota(...); or custom method to reset for new
        // billing cycle
    }

    private void handleCancelled(Subscription subscription) {
        log.info("Handling subscription cancelled for id={}", subscription.getId());
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(Instant.now());
        subscriptionRepository.save(subscription);

        // Downgrade to FREE
        quotaManagementService.updateUserTier(
                subscription.getUser().getId(),
                AccountTier.FREE.name(),
                null);
    }

    private void handleHalted(Subscription subscription) {
        log.info("Handling subscription halted for id={}", subscription.getId());
        subscription.setStatus(SubscriptionStatus.HALTED);
        subscriptionRepository.save(subscription);
        // Policy: Optionally keep tier for grace period, then downgrade via scheduler
    }
}
