package com.ayushsingh.doc_helper.features.payments.service.service_impl;

import com.ayushsingh.doc_helper.features.payments.config.RazorpayProperties;
import com.ayushsingh.doc_helper.features.payments.service.PaymentProviderClient;
import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter over Razorpay Java SDK.
 * Fill in SDK-specific calls using official Razorpay docs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RazorpayPaymentProviderClient implements PaymentProviderClient {

    private final RazorpayProperties properties;

    @Override
    public String createSubscription(BillingPrice price, User user, Long localSubscriptionId) {
        // TODO:
        // 1. Build payload with plan_id = price.getProviderPlanId()
        // 2. Add notes with userId and localSubscriptionId
        // 3. Call Razorpay Subscriptions API using their Java SDK
        // 4. Return provider subscription id
        throw new UnsupportedOperationException("Implement with Razorpay SDK");
    }

    @Override
    public void cancelSubscriptionAtPeriodEnd(String providerSubscriptionId) {
        // TODO: Call Razorpay API to set cancel_at_cycle_end = 1
        throw new UnsupportedOperationException("Implement with Razorpay SDK");
    }

    @Override
    public void cancelSubscriptionImmediately(String providerSubscriptionId) {
        // TODO: Call Razorpay API to cancel subscription immediately
        throw new UnsupportedOperationException("Implement with Razorpay SDK");
    }

    @Override
    public void validateWebhookSignature(String payload, String signatureHeader) {
        // TODO: Use Razorpay's utils to verify webhook signature using
        // properties.getWebhookSecret()
        // Throw runtime exception if invalid
        throw new UnsupportedOperationException("Implement with Razorpay SDK");
    }

    @Override
    public String extractEventId(String payload) {
        // TODO: Parse JSON (e.g., Jackson) and return event id field
        return "unknown-event-id";
    }

    @Override
    public String extractEventType(String payload) {
        // TODO: Parse JSON and return event type field
        return "unknown-event-type";
    }

    @Override
    public String extractSubscriptionId(String payload) {
        // TODO: Parse JSON and return subscription id from payload
        return "unknown-subscription-id";
    }
}
