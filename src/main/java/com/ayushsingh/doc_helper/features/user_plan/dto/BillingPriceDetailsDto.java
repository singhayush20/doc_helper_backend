package com.ayushsingh.doc_helper.features.user_plan.dto;

import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPeriod;
import com.ayushsingh.doc_helper.features.user_plan.entity.Currency;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillingPriceDetailsDto {

    private String priceCode;
    private BillingPeriod billingPeriod;
    private Integer version;
    private Long amount;
    private Currency currency;
    private String providerPlanId;
    private boolean active;
}
