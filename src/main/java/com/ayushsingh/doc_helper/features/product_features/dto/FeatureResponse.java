package com.ayushsingh.doc_helper.features.product_features.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeatureResponse {

    private String code;
    private String name;
    private FeatureUIConfigDto uiConfig;
    private FeatureActionDto action;
    private UsageQuotaDto quota;
}
