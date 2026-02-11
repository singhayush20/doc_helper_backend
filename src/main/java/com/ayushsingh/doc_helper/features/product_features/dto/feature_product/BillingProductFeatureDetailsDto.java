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
public class BillingProductFeatureDetailsDto {
    private Long id;
    private Long productId;
    private Long featureId;
    private Integer enabledVersion;
    private Boolean enabled;
    private Integer priority;
    private Long quotaLimit;
}
