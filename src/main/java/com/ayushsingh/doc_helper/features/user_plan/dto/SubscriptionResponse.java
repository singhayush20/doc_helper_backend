package com.ayushsingh.doc_helper.features.user_plan.dto;

import java.time.Instant;

import com.ayushsingh.doc_helper.features.user_plan.entity.SubscriptionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class SubscriptionResponse {
    private String planCode;
    private String priceCode;
    private SubscriptionStatus status;
    private Boolean cancelAtPeriodEnd;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private String planName;
    private String planTier;
    private Long planMonthlyTokenLimit;
    private Long amount;
    private String currency;
    private String description;
}
