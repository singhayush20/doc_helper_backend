package com.ayushsingh.doc_helper.features.product_features.dto;

import lombok.Data;

@Data
public class FeatureUIUpdateRequest {

    private String icon;
    private String backgroundColor;
    private String textColor;
    private String badgeText;

    private boolean visible;
    private boolean showInPremiumGrid;
}

