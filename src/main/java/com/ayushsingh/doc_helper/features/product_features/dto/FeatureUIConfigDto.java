package com.ayushsingh.doc_helper.features.product_features.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeatureUIConfigDto {

    private String icon;
    private String backgroundColor;
    private String textColor;
    private String badgeText;

    private Integer sortOrder;

    private boolean visible;
    private boolean showInPremiumGrid;
}
