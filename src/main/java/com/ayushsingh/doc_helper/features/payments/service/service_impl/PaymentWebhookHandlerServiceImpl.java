package com.ayushsingh.doc_helper.features.payments.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentProviderEventLog;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentTransaction;
import com.ayushsingh.doc_helper.features.payments.repository.PaymentProviderEventLogRepository;
import com.ayushsingh.doc_helper.features.payments.repository.PaymentTransactionRepository;
import com.ayushsingh.doc_helper.features.payments.service.PaymentProviderClient;
import com.ayushsingh.doc_helper.features.payments.service.PaymentWebhookHandlerService;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import com.ayushsingh.doc_helper.features.user.entity.User;
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
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionFallbackService subscriptionFallbackService;
    private final QuotaManagementService quotaManagementService;

    @Override
    @Transactional
    public void handleProviderEvent(String rawPayload, String signatureHeader) {

        paymentProviderClient.validateWebhookSignature(rawPayload, signatureHeader);

        String eventId = paymentProviderClient.extractEventId(rawPayload);
        String eventType = paymentProviderClient.extractEventType(rawPayload);

        Optional<PaymentProviderEventLog> existing = eventLogRepository.findByProviderEventId(eventId);

        if (existing.isPresent() && existing.get().isProcessed()) {
            log.info("Ignoring duplicate webhook event: {}", eventId);
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

    /**
     * Routes event to payment or subscription handlers.
     */
    private void processEvent(String eventType, String rawPayload) {

        if (eventType.startsWith("payment.") || eventType.startsWith("refund.")) {
            handlePaymentEvent(eventType, rawPayload);
            return;
        }

        handleSubscriptionEvent(eventType, rawPayload);
    }

    /**
     * -------------------------------
     * PAYMENT EVENTS (FINANCIAL LEDGER)
     * -------------------------------
     */
    private void handlePaymentEvent(String eventType, String rawPayload) {

        String providerPaymentId = paymentProviderClient.extractPaymentId(rawPayload);

        if (providerPaymentId == null) {
            log.warn("Payment event without payment id: {}", eventType);
            return;
        }

        if (paymentTransactionRepository
                .existsByProviderPaymentId(providerPaymentId)) {

            log.info("Duplicate payment transaction ignored: {}",
                    providerPaymentId);
            return;
        }

        Optional<String> providerSubId = paymentProviderClient.fetchInvoiceIdForPayment(providerPaymentId)
                .flatMap(paymentProviderClient::fetchSubscriptionIdForInvoice);

        Subscription subscription = providerSubId
                .flatMap(subscriptionRepository::findByProviderSubscriptionId)
                .orElse(null);

        User user = subscription != null
                ? subscription.getUser()
                : null;

        if (user == null || subscription == null) {
            log.error("No local subscription/user found for payment id={}",
                    providerPaymentId);
            throw new BaseException(
                    "No local subscription/user found for payment",
                    ExceptionCodes.SUBSCRIPTION_WEBHOOK_DATA_INVALID_ERROR);
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(user)
                .subscription(subscription)
                .providerPaymentId(providerPaymentId)
                .amount(paymentProviderClient.extractPaymentAmount(rawPayload))
                .currency(paymentProviderClient.extractPaymentCurrency(rawPayload))
                .status(paymentProviderClient.extractPaymentStatus(eventType))
                .type(paymentProviderClient.extractPaymentType(eventType))
                .occurredAt(paymentProviderClient.extractEventTime(rawPayload))
                .rawPayload(rawPayload)
                .build();

        paymentTransactionRepository.save(transaction);

        log.info("Recorded payment transaction: {}", providerPaymentId);
    }

    /**
     * -----------------------------------
     * SUBSCRIPTION EVENTS (ENTITLEMENTS)
     * -----------------------------------
     */
    private void handleSubscriptionEvent(String eventType, String rawPayload) {

        String providerSubId = paymentProviderClient.extractSubscriptionId(rawPayload);

        if (providerSubId == null) {
            log.warn("Subscription event without subscription id: {}", eventType);
            return;
        }

        Subscription subscription = subscriptionRepository
                .findByProviderSubscriptionId(providerSubId)
                .orElse(null);

        if (subscription == null) {
            log.warn("No local subscription found for providerSubId={}", providerSubId);
            return;
        }

        switch (eventType) {

            case "subscription.authenticated" ->
                handleAuthenticated(subscription);

            case "subscription.activated" ->
                handleActivated(subscription);

            case "subscription.pending" ->
                handlePending(subscription);

            case "subscription.halted" ->
                handleHalted(subscription);

            case "subscription.cancelled" ->
                handleCancelled(subscription);

            case "subscription.completed" ->
                handleCompleted(subscription);

            case "subscription.expired" ->
                handleExpired(subscription);

            default ->
                log.info("Unhandled subscription event type: {}", eventType);
        }
    }

    private void handleAuthenticated(Subscription subscription) {

        if (subscription.getStatus() != SubscriptionStatus.INCOMPLETE) {
            return;
        }

        log.info("Subscription authenticated: {}", subscription.getId());

        subscription.setStatus(SubscriptionStatus.INCOMPLETE);
        subscriptionRepository.save(subscription);
    }

    /**
     * Subscription became ACTIVE → apply paid quota.
     */
    private void handleActivated(Subscription subscription) {

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        log.info("Subscription activated: {}", subscription.getId());

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        Long tokenLimit = subscription.getBillingPrice()
                .getProduct()
                .getMonthlyTokenLimit();

        quotaManagementService.applySubscriptionQuota(
                subscription.getUser().getId(),
                tokenLimit);
    }

    private void handlePending(Subscription subscription) {
        // TODO: Take appropriate actions for pending subscriptions

        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            return;
        }

        log.warn("Subscription moved to PAST_DUE: {}", subscription.getId());

        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);
    }
    
    /**
     * Subscription cancelled.
     * Immediate fallback or scheduled fallback handled.
     */
    private void handleCancelled(Subscription subscription) {

        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            return;
        }

        log.info("Subscription cancelled: {}", subscription.getId());

        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(Instant.now());
        subscriptionRepository.save(subscription);

        boolean immediateCancel = Boolean.FALSE.equals(subscription.getCancelAtPeriodEnd())
                || subscription.getCurrentPeriodEnd() == null
                || Instant.now().isAfter(subscription.getCurrentPeriodEnd());

        if (immediateCancel) {
            subscriptionFallbackService.applyFreePlan(
                    subscription.getUser().getId());
        }
    }

    private void handleCompleted(Subscription subscription) {

        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            return;
        }

        log.info("Subscription completed: {}", subscription.getId());

        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(subscription);

        subscriptionFallbackService.applyFreePlan(
                subscription.getUser().getId());
    }    

    private void handleExpired(Subscription subscription) {
        // TODO: Take appropriate actions for expired subscriptions

        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            return;
        }

        log.warn("Subscription expired before activation: {}", subscription.getId());

        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);
    }    

    /**
     * Subscription halted (payment issues, etc.).
     * No quota change here; policy handled via scheduler.
     */
    private void handleHalted(Subscription subscription) {
        // TODO: Take appropriate actions for halted subscriptions
        subscription.setStatus(SubscriptionStatus.HALTED);
        subscriptionRepository.save(subscription);
    }
}
