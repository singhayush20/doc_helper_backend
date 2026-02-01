package com.ayushsingh.doc_helper.features.product_features.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUpdateRequestDto {

    private String name;
    private String description;
    private String type;
}
