package com.ayushsingh.doc_helper.features.product_features.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFeatureDto {
    private String code;
    private String name;
    private FeatureUIConfigDto uiConfig;
    private FeatureActionDto action;
    private UsageQuotaDto quota;
}
