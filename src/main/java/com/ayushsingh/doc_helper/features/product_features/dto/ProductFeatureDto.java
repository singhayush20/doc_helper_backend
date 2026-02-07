package com.ayushsingh.doc_helper.features.product_features.dto;

import com.ayushsingh.doc_helper.features.product_features.entity.UsageMetric;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFeatureDto {
    private FeatureCodes code;
    private String name;
    private UsageQuotaDto quota;
    private Long featureId;
    private UsageMetric usageMetric;
}
