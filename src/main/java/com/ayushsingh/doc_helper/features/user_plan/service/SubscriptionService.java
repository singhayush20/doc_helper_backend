package com.ayushsingh.doc_helper.features.user_plan.service;

import com.ayushsingh.doc_helper.features.payments.dto.CheckoutSessionResponse;
import com.ayushsingh.doc_helper.features.user_plan.entity.Subscription;

public interface SubscriptionService {

    CheckoutSessionResponse startCheckoutForPriceCode(Long userId, String priceCode);

    Subscription getCurrentActiveSubscription(Long userId);

    void cancelCurrentSubscriptionAtPeriodEnd(Long userId);
}