package com.ayushsingh.doc_helper.features.payments.service.service_impl;

import com.ayushsingh.doc_helper.features.payments.entity.PaymentProviderEventLog;
import com.ayushsingh.doc_helper.features.payments.repository.PaymentProviderEventLogRepository;
import com.ayushsingh.doc_helper.features.payments.service.PaymentProviderClient;
import com.ayushsingh.doc_helper.features.payments.service.PaymentWebhookHandlerService;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import com.ayushsingh.doc_helper.features.user_plan.entity.Subscription;
import com.ayushsingh.doc_helper.features.user_plan.entity.SubscriptionStatus;
import com.ayushsingh.doc_helper.features.user_plan.repository.SubscriptionRepository;
import com.ayushsingh.doc_helper.features.user_plan.service.SubscriptionFallbackService;

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
    private final SubscriptionFallbackService subscriptionFallbackService;

    @Override
    @Transactional
    public void handleProviderEvent(String rawPayload, String signatureHeader) {
        paymentProviderClient.validateWebhookSignature(rawPayload, signatureHeader);

        String eventId = paymentProviderClient.extractEventId(rawPayload);
        String eventType = paymentProviderClient.extractEventType(rawPayload);

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

        processEvent(eventType, rawPayload);

        eventLog.setProcessed(true);
        eventLog.setProcessedAt(Instant.now());
        eventLogRepository.save(eventLog);
    }

    private void processEvent(String eventType, String rawPayload) {
        // TODO: Add actual event type strings from Razorpay
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
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        Long tokenLimit = subscription.getBillingPrice()
                .getProduct()
                .getMonthlyTokenLimit();

        quotaManagementService.applySubscriptionQuota(
                subscription.getUser().getId(),
                tokenLimit);
    }

    private void handleCharged(Subscription subscription) {
        quotaManagementService.resetQuotaForNewBillingCycle(
                subscription.getUser().getId());
    }

    private void handleCancelled(Subscription subscription) {

        log.info("Handling subscription cancelled for id={}", subscription.getId());

        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(Instant.now());
        subscriptionRepository.save(subscription);

        // Immediate cancellation: fallback now
        boolean immediateCancel = Boolean.FALSE.equals(subscription.getCancelAtPeriodEnd())
                || subscription.getCurrentPeriodEnd() == null
                || Instant.now().isAfter(subscription.getCurrentPeriodEnd());

        if (immediateCancel) {
            subscriptionFallbackService.applyFreePlan(
                    subscription.getUser().getId());
        }

        // else:
        // cancel_at_period_end = true
        // FREE fallback will be applied by scheduler once period ends
    }

    private void handleHalted(Subscription subscription) {
        log.info("Handling subscription halted for id={}", subscription.getId());
        subscription.setStatus(SubscriptionStatus.HALTED);
        subscriptionRepository.save(subscription);
        // Policy: Optionally keep tier for grace period, then downgrade via scheduler
    }
}
