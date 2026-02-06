package com.ayushsingh.doc_helper.features.product_features.dto.feature_product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingProductFeatureMapRequestDto {
    private Long productId; // product id
    private Long featureId; // feature id
    private Long enabledVersion; // enabled ui version
    private Integer priority; // priority
    private Long quotaLimit; // usage limit for the product feature
}
