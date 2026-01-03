package com.ayushsingh.doc_helper.features.user_plan.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.payments.config.RazorpayProperties;
import com.ayushsingh.doc_helper.features.payments.dto.CheckoutSessionResponse;
import com.ayushsingh.doc_helper.features.payments.service.PaymentProviderClient;
import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user_plan.dto.SubscriptionResponse;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;
import com.ayushsingh.doc_helper.features.user_plan.entity.Subscription;
import com.ayushsingh.doc_helper.features.user_plan.entity.SubscriptionStatus;
import com.ayushsingh.doc_helper.features.user_plan.projection.CurrentSubscriptionView;
import com.ayushsingh.doc_helper.features.user_plan.repository.BillingPriceRepository;
import com.ayushsingh.doc_helper.features.user_plan.repository.SubscriptionRepository;
import com.ayushsingh.doc_helper.features.user_plan.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

        private final BillingPriceRepository billingPriceRepository;
        private final SubscriptionRepository subscriptionRepository;
        private final PaymentProviderClient paymentProviderClient;
        private final RazorpayProperties razorpayProperties;

        @Override
        @Transactional
        public CheckoutSessionResponse startCheckoutForPriceCode(String priceCode) {

                User user = UserContext.getCurrentUser().getUser();

                assertNoActiveSubscription(user);

                BillingPrice price = billingPriceRepository
                                .findFirstByPriceCodeAndActiveTrueOrderByVersionDesc(priceCode)
                                .orElseThrow(() -> new BaseException(
                                                "Plan not found or inactive",
                                                ExceptionCodes.INVALID_PLAN));

                Subscription subscription = Subscription.builder()
                                .user(user)
                                .billingPrice(price)
                                .status(SubscriptionStatus.INCOMPLETE)
                                .cancelAtPeriodEnd(false)
                                .build();

                subscription = subscriptionRepository.save(subscription);

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

        private void assertNoActiveSubscription(User user) {
                subscriptionRepository
                                .findFirstByUserAndStatusInOrderByCreatedAtDesc(
                                                user,
                                                List.of(
                                                                SubscriptionStatus.ACTIVE,
                                                                SubscriptionStatus.INCOMPLETE,
                                                                SubscriptionStatus.PAST_DUE))
                                .ifPresent(sub -> {
                                        throw new BaseException(
                                                        "Active subscription already exists",
                                                        ExceptionCodes.SUBSCRIPTION_ALREADY_EXISTS);
                                });
        }

        @Override
        @Transactional(readOnly = true)
        public SubscriptionResponse getCurrentSubscription() {

                List<CurrentSubscriptionView> result = subscriptionRepository.findCurrentSubscriptionView(
                                UserContext.getCurrentUser().getUser().getId(),
                                List.of(
                                                SubscriptionStatus.ACTIVE,
                                                SubscriptionStatus.PAST_DUE),
                                PageRequest.of(0, 1));

                if (result.isEmpty()) {
                        return new SubscriptionResponse();
                }

                CurrentSubscriptionView view = result.get(0);

                return SubscriptionResponse.builder()
                                .planCode(view.getPlanCode())
                                .priceCode(view.getPriceCode())
                                .status(view.getStatus())
                                .cancelAtPeriodEnd(view.getCancelAtPeriodEnd())
                                .currentPeriodStart(view.getCurrentPeriodStart())
                                .currentPeriodEnd(view.getCurrentPeriodEnd())
                                .planName(view.getPlanName())
                                .planTier(view.getPlanTier().name())
                                .planMonthlyTokenLimit(view.getPlanMonthlyTokenLimit())
                                .amount(view.getAmount())
                                .currency(view.getCurrency())
                                .description(view.getPriceDescription())
                                .build();
        }

        @Override
        @Transactional
        public void cancelCurrentSubscriptionAtPeriodEnd() {
                var user = UserContext.getCurrentUser().getUser();
                var subscription = subscriptionRepository.findFirstByUserAndStatusInOrderByCreatedAtDesc(
                                user, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE)).orElseThrow(()-> new BaseException(
                                        "No subscription found",
                                        ExceptionCodes.SUBSCRIPTION_NOT_FOUND));

                if (subscription.getProviderSubscriptionId() == null) {
                        throw new BaseException(
                                        "Subscription not linked with payment provider",
                                        ExceptionCodes.SUBSCRIPTION_INVALID_STATE);
                }

                if (subscription.getStatus() != SubscriptionStatus.ACTIVE &&
                                subscription.getStatus() != SubscriptionStatus.PAST_DUE) {

                        throw new BaseException(
                                        "Subscription cannot be cancelled in current state: "
                                                        + subscription.getStatus(),
                                        ExceptionCodes.SUBSCRIPTION_INVALID_STATE);
                }

                if (Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())) {
                        throw new BaseException(
                                        "Subscription is already scheduled for cancellation",
                                        ExceptionCodes.SUBSCRIPTION_ALREADY_CANCELLED);
                }

                // Call provider
                paymentProviderClient.cancelSubscriptionAtPeriodEnd(
                                subscription.getProviderSubscriptionId());

                // Update local state
                subscription.setCancelAtPeriodEnd(true);
                subscriptionRepository.save(subscription);
        }
}
