package com.ayushsingh.doc_helper.features.payments.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentProviderEventLog;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentStatus;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentTransaction;
import com.ayushsingh.doc_helper.features.payments.repository.PaymentProviderEventLogRepository;
import com.ayushsingh.doc_helper.features.payments.repository.PaymentTransactionRepository;
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
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionFallbackService subscriptionFallbackService;
    private final QuotaManagementService quotaManagementService;

    @Override
    @Transactional
    public void handleProviderEvent(String rawPayload, String signatureHeader) {
        log.info("Received webhook {}",rawPayload);

        paymentProviderClient.validateWebhookSignature(rawPayload, signatureHeader);

        String eventId = paymentProviderClient.extractEventId(rawPayload);
        String eventType = paymentProviderClient.extractEventType(rawPayload);

        log.info("webhook event: eventId: {} eventType: {}",eventId,eventType);

        Optional<PaymentProviderEventLog> existingEventLog = eventLogRepository.findByProviderEventId(eventId);

        if (existingEventLog.isPresent() && existingEventLog.get().isProcessed()) {
            log.info("Existing event log found for event id: {}, skipping!",eventId);
            return;
        }

        PaymentProviderEventLog eventLog = existingEventLog.orElseGet(() -> PaymentProviderEventLog.builder()
                .providerEventId(eventId)
                .eventType(eventType)
                .receivedAt(Instant.now())
                .processed(false)
                .rawPayload(rawPayload)
                .build());

        log.info("Processing new event log for eventId: {}, eventType: {}",eventId,eventType);

        processEvent(eventType, rawPayload);

        eventLog.setProcessed(true);
        eventLog.setProcessedAt(Instant.now());
        var savedLog =  eventLogRepository.save(eventLog);
        log.info("Saved new event log with id: {}",savedLog.getId());
    }

    /**
     * Routes event to payment or subscription handlers.
     */
    private void processEvent(String eventType, String rawPayload) {
        log.info("Processing new event for eventType: {}, payload: {}",eventType,rawPayload);
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
        log.info("Handling payment event: eventType: {}, rawPayload: {}",eventType,rawPayload);
        String providerPaymentId =
                paymentProviderClient.extractPaymentId(rawPayload);

        log.debug("Obtained providerPaymentId: {} for eventType: {}",providerPaymentId,eventType);

        if (providerPaymentId == null) {
            log.info("Provider payment id is null for eventType: {}",eventType);
            return;
        }


        PaymentStatus newStatus =
                paymentProviderClient.extractPaymentStatus(eventType);
        log.debug("Obtained new payment status: {} for eventType: {}",newStatus,eventType);

        Optional<PaymentTransaction> paymentTransactionOptional =
                paymentTransactionRepository
                        .findByProviderPaymentId(providerPaymentId);

        if (paymentTransactionOptional.isPresent()) {
            log.info("Updating existing paymentTransaction for eventType: {}, providerPlanId: {}, rawPayload: {}",eventType,providerPaymentId,rawPayload);

            PaymentTransaction transaction = paymentTransactionOptional.get();

            if (isFinalState(transaction.getStatus())) {
                log.info("Skipping payment transaction update for eventType: {}, providerPlanId: {}, rawPayload: {}",eventType,providerPaymentId,rawPayload);
                return;
            }

            transaction.setStatus(newStatus);
            transaction.setOccurredAt(
                    paymentProviderClient.extractEventTime(rawPayload));
            transaction.setRawPayload(rawPayload);

            paymentTransactionRepository.save(transaction);
            return;
        }


        /* ---------- first time seeing this payment ---------- */
        Optional<String> invoiceId = paymentProviderClient.fetchInvoiceIdForPayment(providerPaymentId);
        Optional<String> providerSubId =
                invoiceId.flatMap(paymentProviderClient::fetchSubscriptionIdForInvoice);

        log.info("Provider Subscription id obtained for eventType: {}, invoiceId: {}, providerPaymentId: {}",invoiceId, eventType,providerPaymentId);

        Subscription subscription = providerSubId
                .flatMap(subscriptionRepository::findByProviderSubscriptionId)
                .orElse(null);

        if (subscription == null) {
            log.error("Subscription not found for providerSubId: {}, providerPaymentId: {}",providerSubId, providerPaymentId);
            throw new BaseException(
                    "No local subscription found for payment",
                    ExceptionCodes.SUBSCRIPTION_WEBHOOK_DATA_INVALID_ERROR);
        }
        log.info("Subscription found with id: {} for providerSubId: {}",subscription.getId(),providerSubId);


        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(subscription.getUser())
                .subscription(subscription)
                .providerPaymentId(providerPaymentId)
                .amount(paymentProviderClient.extractPaymentAmount(rawPayload))
                .currency(paymentProviderClient.extractPaymentCurrency(rawPayload))
                .status(newStatus)
                .type(paymentProviderClient.extractPaymentType(eventType))
                .occurredAt(paymentProviderClient.extractEventTime(rawPayload))
                .rawPayload(rawPayload)
                .build();

        paymentTransactionRepository.save(transaction);

        log.info("Saved new PaymentTransaction record for providerPaymentId: {}, providerSubId: {}",providerPaymentId,providerSubId);
    }

    private boolean isFinalState(PaymentStatus status) {
        return status == PaymentStatus.SUCCEEDED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.REFUNDED;
    }

    /**
     * -----------------------------------
     * SUBSCRIPTION EVENTS (ENTITLEMENTS)
     * -----------------------------------
     */
    private void handleSubscriptionEvent(String eventType, String rawPayload) {

        String providerSubId = paymentProviderClient.extractSubscriptionId(rawPayload);
        if (providerSubId == null) {
            return;
        }

        Subscription subscription = subscriptionRepository
                .findByProviderSubscriptionId(providerSubId)
                .orElse(null);

        if (subscription == null) {
            return;
        }

        switch (eventType) {

            // Sent when the first payment is made on the subscription. This can either be the authorisation amount,
            // the upfront amount, the plan amount or a combination of the plan amount and the upfront amount.
            // WE DON'T NEED TO MAP THIS STATE AS THE SUBSCRIPTION IS STILL NOT ACTIVE
            case "subscription.authenticated" -> handleAuthenticated(subscription);
            // Sent when the subscription moves to the active state either from the authenticated ,
            // pending or halted state. If a Subscription moves to the active state from the pending or halted state,
            // only the subsequent invoices that are generated are charged.
            // Existing invoices that were already generated are not charged.
            case "subscription.activated" -> handleActivated(subscription, rawPayload);
            // Sent every time a successful charge is made on the subscription.
            // This is mapped to ACTIVE SubscriptionStatus and used to reset quotas and fetch active subscription for users
            case "subscription.charged" -> handleSubscriptionCharged(subscription, rawPayload);
            // Sent when the subscription moves to the pending state. This happens when a charge on the card fails.
            // We try to charge the card on a periodic basis while it is in the pending state.
            // If the payment fails again, the Webhook is triggered again.
            case "subscription.pending" -> handlePending(subscription);
            // Sent when all retries have been exhausted and the subscription moves from the pending state to the halted state.
            // The customer has to manually retry the charge or change the card linked to the subscription,
            // for the subscription to move back to the active state.
            case "subscription.halted" -> handleHalted(subscription);
            // Sent when a subscription is cancelled and moved to the cancelled state.
            case "subscription.cancelled" -> handleCancelled(subscription);
            case "subscription.completed" -> handleCompleted(subscription, rawPayload);
            // "subscription.paused" and "subscription.resumed" are not handled because this feature is provided by the application
            default -> log.info("Unhandled subscription event type: {}", eventType);
        }
    }


    private void handleAuthenticated(Subscription subscription) {

        if (subscription.getStatus() != SubscriptionStatus.INCOMPLETE) {
            return;
        }

        subscription.setStatus(SubscriptionStatus.INCOMPLETE);
        subscriptionRepository.save(subscription);
    }

    private void handleSubscriptionCharged(Subscription subscription, String rawPayload) {

        Instant start =
                paymentProviderClient.extractSubscriptionPeriodStart(rawPayload);
        Instant end =
                paymentProviderClient.extractSubscriptionPeriodEnd(rawPayload);

        if (start != null) subscription.setCurrentPeriodStart(start);
        if (end != null) subscription.setCurrentPeriodEnd(end);

        subscription.setStatus(SubscriptionStatus.ACTIVE);

        subscriptionRepository.save(subscription);

        Long tokenLimit = subscription.getBillingPrice()
                .getProduct()
                .getMonthlyTokenLimit();

        quotaManagementService.applySubscriptionQuota(
                subscription.getUser().getId(),
                tokenLimit);
    }

    private void handleCancelled(Subscription subscription) {

        if (subscription.getStatus() == SubscriptionStatus.CANCELED) return;

        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(Instant.now());
        subscriptionRepository.save(subscription);

        boolean immediateCancel =
                subscription.getCurrentPeriodEnd() == null
                        || Instant.now().isAfter(subscription.getCurrentPeriodEnd());

        if (immediateCancel) {
            subscriptionFallbackService.applyFreePlan(
                    subscription.getUser().getId());
        }
    }


    private void handleCompleted(Subscription subscription, String rawPayload) {
        Instant end =
                paymentProviderClient.extractSubscriptionPeriodEnd(rawPayload);

        if (end != null) subscription.setCurrentPeriodEnd(end);

        subscription.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(subscription);

    }

    private void handleActivated(Subscription subscription, String rawPayload) {

        // ACTIVE is only set on subscription.charged
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return; // already paid, nothing to do
        }

        if (subscription.getStatus() == SubscriptionStatus.CREATED) {
            return; // already in correct state
        }

        subscription.setStatus(SubscriptionStatus.CREATED);
        subscriptionRepository.save(subscription);
    }


    private void handlePending(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            return;
        }

        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);
    }

    private void handleHalted(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.HALTED) {
            return;
        }
        subscription.setStatus(SubscriptionStatus.HALTED);
        subscriptionRepository.save(subscription);
    }
}
