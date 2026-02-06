package com.ayushsingh.doc_helper.features.user_plan.service.service_impl;

import org.springframework.stereotype.Service;

import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import com.ayushsingh.doc_helper.features.user_plan.service.SubscriptionFallbackService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionFallbackServiceImpl implements SubscriptionFallbackService {

    private final QuotaManagementService quotaManagementService;


    @Override
    public void applyFreePlan(Long userId) {

        quotaManagementService.applyFreeQuota(userId);
    }
}
