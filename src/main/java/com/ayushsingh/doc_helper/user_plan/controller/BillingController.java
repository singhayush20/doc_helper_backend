package com.ayushsingh.doc_helper.user_plan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.payments.dto.CheckoutSessionResponse;
import com.ayushsingh.doc_helper.user_plan.dto.SubscriptionResponse;
import com.ayushsingh.doc_helper.user_plan.entity.Subscription;
import com.ayushsingh.doc_helper.user_plan.service.SubscriptionService;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/razorpay/checkout")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @RequestParam("priceCode") String priceCode) {

        Long userId = UserContext.getCurrentUser().getUser().getId();
        CheckoutSessionResponse response = subscriptionService.startCheckoutForPriceCode(userId, priceCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subscription/current")
public ResponseEntity<SubscriptionResponse> getCurrentSubscription() {
    Long userId = UserContext.getCurrentUser().getUser().getId();

    Subscription sub = subscriptionService.getCurrentActiveSubscription(userId);
    if (sub == null) {
        return ResponseEntity.ok(null);
    }

    return ResponseEntity.ok(
            SubscriptionResponse.builder()
                    .planCode(sub.getBillingPrice().getProduct().getCode())
                    .priceCode(sub.getBillingPrice().getPriceCode())
                    .status(sub.getStatus())
                    .cancelAtPeriodEnd(sub.getCancelAtPeriodEnd())
                    .currentPeriodStart(sub.getCurrentPeriodStart())
                    .currentPeriodEnd(sub.getCurrentPeriodEnd())
                    .build()
    );
}

    @PostMapping("/subscription/cancel")
    public ResponseEntity<Void> cancelCurrentAtPeriodEnd() {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        subscriptionService.cancelCurrentSubscriptionAtPeriodEnd(userId);
        return ResponseEntity.ok().build();
    }
}
