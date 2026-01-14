package com.ayushsingh.doc_helper.features.payments.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import com.ayushsingh.doc_helper.features.payments.entity.PaymentStatus;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentType;
import com.ayushsingh.doc_helper.features.payments.entity.ProviderSubscriptionStatus;
import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPeriod;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;

public interface PaymentProviderClient {

    String createPlan(BillingPeriod billingPeriod, BigDecimal amount, String billingProductCode, String currency,
            String priceCode, String planDescription, Integer version);

    // Create provider subscription and return its id (and any other info if needed)
    String createSubscription(BillingPrice price, User user, Long localSubscriptionId, BillingPeriod billingPeriod);

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

    Instant extractEventTime(String payload);

    ProviderSubscriptionStatus fetchSubscriptionStatus(String providerSubscriptionId);

    Optional<String> fetchInvoiceIdForPayment(String providerPaymentId);

    Optional<String> fetchSubscriptionIdForInvoice(String providerInvoiceId);

    Instant extractSubscriptionPeriodStart(String payload);

    Instant extractSubscriptionPeriodEnd(String payload);

}
