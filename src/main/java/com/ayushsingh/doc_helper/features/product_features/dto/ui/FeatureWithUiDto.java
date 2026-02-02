package com.ayushsingh.doc_helper.features.product_features.dto.ui;

import com.ayushsingh.doc_helper.features.product_features.dto.ProductFeatureDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureWithUiDto {

    private ProductFeatureDto feature;
    private JsonNode ui;
}
