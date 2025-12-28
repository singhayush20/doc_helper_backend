package com.ayushsingh.doc_helper.features.user_plan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ayushsingh.doc_helper.features.payments.dto.CheckoutSessionResponse;
import com.ayushsingh.doc_helper.features.user_plan.dto.SubscriptionResponse;
import com.ayushsingh.doc_helper.features.user_plan.service.SubscriptionService;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @RequestParam("priceCode") String priceCode) {

        CheckoutSessionResponse response = subscriptionService.startCheckoutForPriceCode(priceCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subscription/current")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription() {

        var subscriptionResponse = subscriptionService.getCurrentSubscription();
        return ResponseEntity.ok(subscriptionResponse);
    }

    @PostMapping("/subscription/cancel")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<Void> cancelCurrentAtPeriodEnd() {
        subscriptionService.cancelCurrentSubscriptionAtPeriodEnd();
        return ResponseEntity.ok().build();
    }
}
