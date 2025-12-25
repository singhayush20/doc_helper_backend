package com.ayushsingh.doc_helper.user_plan.dto;

import com.ayushsingh.doc_helper.user_plan.entity.AccountTier;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
