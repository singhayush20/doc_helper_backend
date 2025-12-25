package com.ayushsingh.doc_helper.features.user_plan.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String code;
    private String displayName;
    private AccountTier tier;
    private Long monthlyTokenLimit;
    private boolean active;

    private List<PriceResponse> prices;
}
