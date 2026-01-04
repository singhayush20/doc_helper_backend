package com.ayushsingh.doc_helper.features.payments.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.payments.config.RazorpayProperties;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentStatus;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentType;
import com.ayushsingh.doc_helper.features.payments.service.PaymentProviderClient;
import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPeriod;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Plan;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Subscription;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RazorpayPaymentProviderClient implements PaymentProviderClient {

    private final RazorpayProperties properties;
    private final RazorpayClient razorpayClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String createPlan(
            BillingPeriod billingPeriod,
            BigDecimal amount,
            String billingProductCode,
            String currency,
            String priceCode,
            String planDescription,
            Integer version) {
        JSONObject planRequest = new JSONObject();
        planRequest.put("period", billingPeriod.getPeriod());
        planRequest.put("interval", 1);
        planRequest.put("item", new JSONObject()
                .put("name", priceCode)
                .put("amount", getAmountInSubunits(amount))
                .put("currency",
                        currency)
                .put("description", planDescription));
        JSONObject notes = new JSONObject();
        notes.put("version", version);
        notes.put("billingProductCode", billingProductCode);
        planRequest.put("notes", notes);

        Plan plan;
        try {
            plan = razorpayClient.plans.create(planRequest);
            String providerPlanId = plan.get("id");
            return providerPlanId;
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay plan: {}", e.getMessage());
            e.printStackTrace();
            throw new BaseException(
                    "Failed to create plan with payment provider",
                    ExceptionCodes.PLAN_GENERATION_ERROR);
        }
    }

    private Long getAmountInSubunits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    @Override
    public String createSubscription(
            BillingPrice price,
            User user,
            Long localSubscriptionId) {

        JSONObject payload = new JSONObject();
        payload.put("plan_id", price.getProviderPlanId());
        payload.put("total_count", 1);

        JSONObject notes = new JSONObject();
        notes.put("user_id", user.getId());
        notes.put("local_subscription_id", localSubscriptionId);

        payload.put("notes", notes);

        Subscription sub;
        try {
            sub = razorpayClient.subscriptions.create(payload);
            return sub.get("id");
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay subscription for userId={}, localSubscriptionId={}: {}",
                    user.getId(), localSubscriptionId, e.getMessage());

            throw new BaseException(
                    "Failed to create subscription with payment provider",
                    ExceptionCodes.PAYMENT_PROVIDER_ERROR);
        }
    }

    @Override
    public void cancelSubscriptionAtPeriodEnd(String providerSubscriptionId) {
        try {
            JSONObject params = new JSONObject();
            params.put("cancel_at_cycle_end", true);

            razorpayClient.subscriptions.cancel(providerSubscriptionId, params);

        } catch (RazorpayException ex) {
            log.error("Failed to cancel Razorpay subscription at period end: {}",
                    providerSubscriptionId, ex);

            throw new BaseException(
                    "Failed to cancel subscription at period end",
                    ExceptionCodes.PAYMENT_PROVIDER_ERROR);
        }
    }

    @Override
    public void cancelSubscriptionImmediately(String providerSubscriptionId) {
        try {
            JSONObject params = new JSONObject();
            params.put("cancel_at_cycle_end", false);

            razorpayClient.subscriptions.cancel(providerSubscriptionId, params);

        } catch (RazorpayException ex) {
            log.error("Failed to cancel Razorpay subscription immediately: {}",
                    providerSubscriptionId, ex);

            throw new BaseException(
                    "Failed to cancel subscription immediately",
                    ExceptionCodes.PAYMENT_PROVIDER_ERROR);
        }
    }

    @Override
    public void validateWebhookSignature(String payload, String signatureHeader) {
        boolean valid = false;
        try {
            valid = Utils.verifyWebhookSignature(
                    payload,
                    signatureHeader,
                    properties.webhookSecret());
        } catch (RazorpayException e) {
            log.error("Error verifying webhook signature, {}: {}", e, e.getMessage());
        } finally {
            if (!valid) {
                throw new BaseException(
                        "Invalid Razorpay webhook signature",
                        ExceptionCodes.INVALID_WEBHOOK_SIGNATURE);
            }
        }
    }

    @Override
    public String extractEventId(String payload) {
        try {
            return mapper.readTree(payload).get("id").asText();
        } catch (JsonProcessingException e) {
            log.error("Error extracting event id from razorpay event: {}: {}", e, e.getMessage());
            e.printStackTrace();
            throw new BaseException(
                    "Error when parsing webhook payload",
                    ExceptionCodes.PAYMENT_PROVIDER_ERROR);
        }
    }

    @Override
    public String extractEventType(String payload) {
        try {
            return mapper.readTree(payload).get("event").asText();
        } catch (JsonProcessingException e) {
            log.error("Error extracting event type from razorpay event: {}: {}", e, e.getMessage());
            e.printStackTrace();
            throw new BaseException(
                    "Error when parsing webhook payload",
                    ExceptionCodes.PAYMENT_PROVIDER_ERROR);
        }
    }

    @Override
    public String extractSubscriptionId(String payload) {
        JsonNode node;
        try {
            node = mapper.readTree(payload);
            return node.at("/payload/subscription/entity/id").asText(null);
        } catch (JsonProcessingException e) {
            log.error("Error extracting subscription id from razorpay event: {}: {}", e, e.getMessage());
            e.printStackTrace();
            throw new BaseException(
                    "Error when parsing webhook payload",
                    ExceptionCodes.PAYMENT_PROVIDER_ERROR);
        }
    }

    @Override
    public String extractPaymentId(String payload) {
        return new JSONObject(payload)
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity")
                .getString("id");
    }

    @Override
    public BigDecimal extractPaymentAmount(String payload) {
        long amountPaise = new JSONObject(payload)
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity")
                .getLong("amount");

        return BigDecimal.valueOf(amountPaise)
                .divide(BigDecimal.valueOf(100));
    }

    @Override
    public String extractPaymentCurrency(String payload) {
        return new JSONObject(payload)
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity")
                .getString("currency");
    }

    @Override
    public PaymentStatus extractPaymentStatus(String eventType) {
        return eventType.equals("payment.captured")
                ? PaymentStatus.SUCCEEDED
                : PaymentStatus.FAILED;
    }

    @Override
    public PaymentType extractPaymentType(String eventType) {
        return eventType.contains("refund")
                ? PaymentType.REFUND
                : PaymentType.SUBSCRIPTION;
    }

    @Override
    public Long extractUserIdFromNotes(String payload) {
        return new JSONObject(payload)
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity")
                .getJSONObject("notes")
                .optLong("userId");
    }

    @Override
    public Long extractSubscriptionIdFromNotes(String payload) {
        return new JSONObject(payload)
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity")
                .getJSONObject("notes")
                .optLong("subscriptionId");
    }

    @Override
    public Instant extractEventTime(String payload) {
        long epoch = new JSONObject(payload)
                .getLong("created_at");
        return Instant.ofEpochSecond(epoch);
    }

}
