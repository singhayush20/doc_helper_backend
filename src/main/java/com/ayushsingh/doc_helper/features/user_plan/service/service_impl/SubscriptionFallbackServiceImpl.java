package com.ayushsingh.doc_helper.features.user_plan.service.service_impl;

import org.springframework.stereotype.Service;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingProduct;
import com.ayushsingh.doc_helper.features.user_plan.repository.BillingProductRepository;
import com.ayushsingh.doc_helper.features.user_plan.service.SubscriptionFallbackService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionFallbackServiceImpl implements SubscriptionFallbackService {

    private final BillingProductRepository billingProductRepository;
    private final QuotaManagementService quotaManagementService;

    @Override
    public void applyFreePlan(Long userId) {

        BillingProduct freeProduct = billingProductRepository
                .findByTier(AccountTier.FREE)
                .orElseThrow(() -> new BaseException("Failed to create free plan for user",
                        ExceptionCodes.FREE_TIER_NOT_FOUND));

        quotaManagementService.applySubscriptionQuota(
                userId,
                freeProduct.getMonthlyTokenLimit());
    }
}
