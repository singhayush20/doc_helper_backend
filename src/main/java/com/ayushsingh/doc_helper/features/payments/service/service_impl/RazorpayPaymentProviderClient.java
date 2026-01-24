package com.ayushsingh.doc_helper.features.payments.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.payments.config.RazorpayProperties;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentStatus;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentType;
import com.ayushsingh.doc_helper.features.payments.entity.ProviderSubscriptionStatus;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

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
            return plan.get("id");
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay plan: {}", e.getMessage());
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
            Long localSubscriptionId, BillingPeriod billingPeriod) {

        JSONObject payload = new JSONObject();
        payload.put("plan_id", price.getProviderPlanId());
        payload.put("total_count", getTotalCount(billingPeriod));

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

    private int getTotalCount(BillingPeriod billingPeriod) {
        return switch(billingPeriod) {
            case BillingPeriod.MONTHLY -> 12;
            case BillingPeriod.QUATERLY -> 4;
            case BillingPeriod.YEARLY -> 1;
        };
    }

    @Override
    public void cancelSubscriptionAtPeriodEnd(String providerSubscriptionId) {
        try {
            JSONObject params = new JSONObject();
            params.put("cancel_at_cycle_end", true);

            var updatedSubscription = razorpayClient.subscriptions.cancel(providerSubscriptionId, params);

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
                    properties.subscriptionWebhookSecret());
        } catch (RazorpayException e) {
            log.error("Error verifying webhook signature, {}: {}", e, e.getMessage());
        }
        if (!valid) {
            throw new BaseException(
                    "Invalid Razorpay webhook signature",
                    ExceptionCodes.INVALID_WEBHOOK_SIGNATURE);
        }
    }

    @Override
    public String extractEventId(String payload) {
        try {
            JsonNode root = mapper.readTree(payload);

            String eventType = root.path("event").asText(null);
            long createdAt = root.path("created_at").asLong(0);

            String resourceId = null;

            if (eventType.startsWith("payment.")) {
                resourceId = root.at("/payload/payment/entity/id").asText(null);
            } else if (eventType.startsWith("subscription.")) {
                resourceId = root.at("/payload/subscription/entity/id").asText(null);
            } else if (eventType.startsWith("refund.")) {
                resourceId = root.at("/payload/refund/entity/id").asText(null);
            }

            if (resourceId == null || createdAt == 0) {
                return null;
            }

            return eventType + ":" + resourceId + ":" + createdAt;

        } catch (Exception e) {
            throw new BaseException(
                    "Invalid webhook payload",
                    ExceptionCodes.PAYMENT_PROVIDER_ERROR);
        }
    }


    @Override
    public String extractEventType(String payload) {
        try {
            return mapper.readTree(payload)
                    .path("event")
                    .asText(null);
        } catch (Exception e) {
            throw new BaseException(
                    "Invalid webhook payload",
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
            throw new BaseException(
                    "Error when parsing webhook payload",
                    ExceptionCodes.PAYMENT_PROVIDER_ERROR);
        }
    }

    private JsonNode paymentNode(String payload) throws Exception {
        return mapper.readTree(payload)
                .path("payload")
                .path("payment")
                .path("entity");
    }

    @Override
    public String extractPaymentId(String payload) {
        try {
            return paymentNode(payload)
                    .path("id")
                    .asText(null);
        } catch (Exception e) {
            log.error("Error extracting id from payload: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public BigDecimal extractPaymentAmount(String payload) {
        try {
            long paise = paymentNode(payload)
                    .path("amount")
                    .asLong(0);

            return BigDecimal.valueOf(paise)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("Error extracting payment amount from payload: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    public String extractPaymentCurrency(String payload) {
        try {
            return paymentNode(payload)
                    .path("currency")
                    .asText(null);
        } catch (Exception e) {
            log.error("Error extracting currency from payload: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Optional<String> fetchInvoiceIdForPayment(String providerPaymentId) {

        try {
            com.razorpay.Payment payment = razorpayClient.payments.fetch(providerPaymentId);

            String invoiceId = payment.get("invoice_id");
            return Optional.ofNullable(invoiceId);

        } catch (RazorpayException e) {
            log.error("Failed to fetch payment {}", providerPaymentId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> fetchSubscriptionIdForInvoice(
            String providerInvoiceId) {

        try {
            com.razorpay.Invoice invoice = razorpayClient.invoices.fetch(providerInvoiceId);

            String subscriptionId = invoice.get("subscription_id");
            return Optional.ofNullable(subscriptionId);

        } catch (RazorpayException e) {
            log.error("Failed to fetch invoice {}", providerInvoiceId, e);
            return Optional.empty();
        }
    }

    @Override
    public PaymentStatus extractPaymentStatus(String eventType) {
        // Razorpay defines three payment event types: captured, failed, pending
        return switch (eventType) {
            case "payment.captured"   -> PaymentStatus.SUCCEEDED;
            case "payment.failed"     -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING; // payment.authorized or anything else
        };
    }


    @Override
    public PaymentType extractPaymentType(String eventType) {
        return eventType.contains("refund")
                ? PaymentType.REFUND
                : PaymentType.SUBSCRIPTION;
    }

    @Override
    public Instant extractEventTime(String payload) {
        try {
            long epoch = mapper.readTree(payload)
                    .path("created_at")
                    .asLong(0);
            return Instant.ofEpochSecond(epoch);
        } catch (Exception e) {
            return Instant.now();
        }
    }

    @Override
    public ProviderSubscriptionStatus fetchSubscriptionStatus(
            String providerSubscriptionId) {

        try {
            Subscription subscription = razorpayClient.subscriptions.fetch(providerSubscriptionId);

            String status = subscription.get("status");

            return mapStatus(status);

        } catch (RazorpayException ex) {
            log.error(
                    "Failed to fetch Razorpay subscription status for {}",
                    providerSubscriptionId,
                    ex);
            return ProviderSubscriptionStatus.UNKNOWN;
        }
    }

    @Override
    public Instant extractSubscriptionPeriodStart(String payload) {
        try {
            long epoch = mapper.readTree(payload)
                    .at("/payload/subscription/entity/current_start")
                    .asLong(0);
            return epoch > 0 ? Instant.ofEpochSecond(epoch) : null;
        } catch (Exception e) {
            log.error("Failed to extract subscription current_start", e);
            return null;
        }
    }

    @Override
    public Instant extractSubscriptionPeriodEnd(String payload) {
        try {
            long epoch = mapper.readTree(payload)
                    .at("/payload/subscription/entity/current_end")
                    .asLong(0);
            return epoch > 0 ? Instant.ofEpochSecond(epoch) : null;
        } catch (Exception e) {
            log.error("Failed to extract subscription current_end", e);
            return null;
        }
    }

    private ProviderSubscriptionStatus mapStatus(String razorpayStatus) {

        return switch (razorpayStatus.toLowerCase()) {

            case "created" -> ProviderSubscriptionStatus.CREATED;

            case "authenticated" -> ProviderSubscriptionStatus.AUTHENTICATED;

            case "active" -> ProviderSubscriptionStatus.ACTIVE;

            case "pending" -> ProviderSubscriptionStatus.PENDING;

            case "halted" -> ProviderSubscriptionStatus.HALTED;

            case "cancelled" -> ProviderSubscriptionStatus.CANCELLED;

            case "expired" -> ProviderSubscriptionStatus.EXPIRED;

            case "completed" -> ProviderSubscriptionStatus.COMPLETED;

            default -> ProviderSubscriptionStatus.UNKNOWN;
        };
    }

}
