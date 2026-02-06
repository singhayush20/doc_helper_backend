package com.ayushsingh.doc_helper.features.product_features.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureResponse {

    private List<ProductFeatureDto> features;
}
