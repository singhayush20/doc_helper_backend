package com.ayushsingh.doc_helper.features.product_features.dto;

import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.google.auto.value.AutoValue.Builder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageInfoResponse {
    private FeatureCodes code;
    private String name;
    private Long featureId;
    private UsageQuotaDto usageInfo;
}
