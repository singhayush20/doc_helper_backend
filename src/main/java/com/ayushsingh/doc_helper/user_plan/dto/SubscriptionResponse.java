package com.ayushsingh.doc_helper.user_plan.dto;

import java.time.Instant;

import com.ayushsingh.doc_helper.user_plan.entity.SubscriptionStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionResponse {
    private String planCode;
    private String priceCode;
    private SubscriptionStatus status;
    private Boolean cancelAtPeriodEnd;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
}
