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
public class BillingProductFeatureKeyRequestDto {
    private Long productId;
    private Long featureId;
}
