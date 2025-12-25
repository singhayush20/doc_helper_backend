package com.ayushsingh.doc_helper.features.payments.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.ayushsingh.doc_helper.features.payments.entity.PaymentStatus;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentType;
import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;

public interface PaymentProviderClient {

    // Create provider subscription and return its id (and any other info if needed)
    String createSubscription(BillingPrice price, User user, Long localSubscriptionId);

    // Cancel at period end
    void cancelSubscriptionAtPeriodEnd(String providerSubscriptionId);

    // Optional immediate cancel
    void cancelSubscriptionImmediately(String providerSubscriptionId);

    // Validate webhook signature (throws if invalid)
    void validateWebhookSignature(String payload, String signatureHeader);

    // Event id extraction from raw payload
    String extractEventId(String payload);

    // Event type extraction from raw payload
    String extractEventType(String payload);

    // Extract provider subscription id from a given event payload
    String extractSubscriptionId(String payload);

    String extractPaymentId(String payload);

    BigDecimal extractPaymentAmount(String payload);

    String extractPaymentCurrency(String payload);

    PaymentStatus extractPaymentStatus(String eventType);

    PaymentType extractPaymentType(String eventType);

    Long extractUserIdFromNotes(String payload);

    Long extractSubscriptionIdFromNotes(String payload);

    Instant extractEventTime(String payload);
}
