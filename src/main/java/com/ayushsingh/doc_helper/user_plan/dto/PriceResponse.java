package com.ayushsingh.doc_helper.user_plan.dto;

import com.ayushsingh.doc_helper.user_plan.entity.BillingPeriod;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PriceResponse {

    private Long id;
    private String priceCode;
    private BillingPeriod billingPeriod;
    private Integer version;
    private Long amount; // frontend-friendly
    private String currency;
    private String providerPlanId;
    private boolean active;
}
