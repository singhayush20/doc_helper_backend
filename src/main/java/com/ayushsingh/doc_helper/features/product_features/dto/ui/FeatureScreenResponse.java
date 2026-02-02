package com.ayushsingh.doc_helper.features.product_features.dto.ui;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FeatureScreenResponse {

    private final List<FeatureWithUiDto> features;
}

