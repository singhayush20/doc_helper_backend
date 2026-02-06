package com.ayushsingh.doc_helper.features.product_features.dto.ui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class FeatureScreenResponse {

    private final List<FeatureWithUiDto> features;
}

