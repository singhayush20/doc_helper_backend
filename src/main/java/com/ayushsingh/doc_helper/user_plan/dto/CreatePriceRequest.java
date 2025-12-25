package com.ayushsingh.doc_helper.user_plan.dto;

import com.ayushsingh.doc_helper.user_plan.entity.BillingPeriod;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePriceRequest {
    private String priceCode;
    private Long amount;
    private String currency;
    private Integer intervalMonths;
    private Long tokenLimit;
    private BillingPeriod billingPeriod;
    private String providerPlanId;
    private Integer version;
}
