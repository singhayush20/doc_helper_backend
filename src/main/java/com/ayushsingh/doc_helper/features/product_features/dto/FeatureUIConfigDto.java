package com.ayushsingh.doc_helper.features.product_features.dto;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class FeatureUIConfigDto {

    private String icon;
    private String backgroundColor;
    private String textColor;
    private String badgeText;

    private Integer sortOrder;

    private boolean visible;
}
