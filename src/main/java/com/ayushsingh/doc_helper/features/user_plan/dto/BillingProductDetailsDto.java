package com.ayushsingh.doc_helper.features.user_plan.dto;

import lombok.Builder;
import lombok.Getter;

import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;

@Getter
@Builder
public class BillingProductDetailsDto {

    private Long id;
    private String code;
    private String displayName;
    private AccountTier tier;
    private Long monthlyTokenLimit;
    private boolean active;
}
