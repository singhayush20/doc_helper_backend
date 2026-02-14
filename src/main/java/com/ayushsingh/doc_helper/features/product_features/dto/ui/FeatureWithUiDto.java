package com.ayushsingh.doc_helper.features.product_features.dto.ui;

import java.util.Map;

import com.ayushsingh.doc_helper.features.product_features.dto.ProductFeatureDto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureWithUiDto {

    private ProductFeatureDto feature;
    private Map<String,Object> ui;
}
