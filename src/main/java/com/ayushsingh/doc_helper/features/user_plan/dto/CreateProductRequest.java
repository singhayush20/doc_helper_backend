package com.ayushsingh.doc_helper.features.user_plan.dto;

import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    private String code;
    private String displayName;
    private AccountTier tier;
    private Long monthlyTokenLimit;
    private List<String> features;
}
