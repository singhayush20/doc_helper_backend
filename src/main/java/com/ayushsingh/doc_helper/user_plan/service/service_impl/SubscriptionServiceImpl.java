package com.ayushsingh.doc_helper.user_plan.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.payments.config.RazorpayProperties;
import com.ayushsingh.doc_helper.features.payments.dto.CheckoutSessionResponse;
import com.ayushsingh.doc_helper.features.payments.service.PaymentProviderClient;
import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user.repository.UserRepository;
import com.ayushsingh.doc_helper.user_plan.entity.BillingPrice;
import com.ayushsingh.doc_helper.user_plan.entity.Subscription;
import com.ayushsingh.doc_helper.user_plan.entity.SubscriptionStatus;
import com.ayushsingh.doc_helper.user_plan.repository.BillingPriceRepository;
import com.ayushsingh.doc_helper.user_plan.repository.SubscriptionRepository;
import com.ayushsingh.doc_helper.user_plan.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final BillingPriceRepository billingPriceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentProviderClient paymentProviderClient;
    private final RazorpayProperties razorpayProperties;

    @Override
    @Transactional
    public CheckoutSessionResponse startCheckoutForPriceCode(Long userId, String priceCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException("User not found",
                        ExceptionCodes.USER_NOT_FOUND));

        BillingPrice price = billingPriceRepository
                .findFirstByPriceCodeAndActiveTrueOrderByVersionDesc(priceCode)
                .orElseThrow(() -> new BaseException("Plan not found or inactive",
                        ExceptionCodes.INVALID_PLAN));

        // create local subscription in INCOMPLETE status
        Subscription subscription = Subscription.builder()
                .user(user)
                .billingPrice(price)
                .status(SubscriptionStatus.INCOMPLETE)
                .cancelAtPeriodEnd(false)
                .build();

        subscription = subscriptionRepository.save(subscription);

        // call provider to create subscription
        String providerSubId = paymentProviderClient.createSubscription(price, user, subscription.getId());
        subscription.setProviderSubscriptionId(providerSubId);
        subscriptionRepository.save(subscription);

        return CheckoutSessionResponse.builder()
                .providerSubscriptionId(providerSubId)
                .providerKeyId(razorpayProperties.keyId())
                .planCode(price.getProduct().getCode())
                .priceCode(price.getPriceCode())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Subscription getCurrentActiveSubscription(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException("User not found",
                        ExceptionCodes.USER_NOT_FOUND));

        return subscriptionRepository.findFirstByUserAndStatusInOrderByCreatedAtDesc(
                user,
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE, SubscriptionStatus.INCOMPLETE))
                .orElse(null);
    }

    @Override
    @Transactional
    public void cancelCurrentSubscriptionAtPeriodEnd(Long userId) {
        Subscription subscription = getCurrentActiveSubscription(userId);
        if (subscription == null || subscription.getProviderSubscriptionId() == null) {
            throw new BaseException("No active subscription found",
                    ExceptionCodes.SUBSCRIPTION_NOT_FOUND);
        }

        paymentProviderClient.cancelSubscriptionAtPeriodEnd(subscription.getProviderSubscriptionId());
        subscription.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(subscription);
    }
}
