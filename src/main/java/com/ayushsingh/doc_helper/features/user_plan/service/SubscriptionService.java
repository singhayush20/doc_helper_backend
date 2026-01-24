package com.ayushsingh.doc_helper.features.user_plan.service;

import com.ayushsingh.doc_helper.features.payments.dto.CheckoutSessionResponse;
import com.ayushsingh.doc_helper.features.user_plan.dto.SubscriptionCancelRequest;
import com.ayushsingh.doc_helper.features.user_plan.dto.SubscriptionResponse;

public interface SubscriptionService {

    CheckoutSessionResponse startCheckoutForPriceCode(String priceCode);

    SubscriptionResponse getCurrentSubscription();

    void cancelCurrentSubscriptionAtPeriodEnd();

    void cancelCheckout(SubscriptionCancelRequest request);
}