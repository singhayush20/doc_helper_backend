package com.ayushsingh.doc_helper.features.product_features.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUIUpdateRequest {

    private String icon;
    private String backgroundColor;
    private String textColor;
    private String badgeText;

    private boolean visible;
}

