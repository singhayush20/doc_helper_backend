package com.ayushsingh.doc_helper.features.product_features.dto;

import com.ayushsingh.doc_helper.features.product_features.entity.ActionKind;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeatureActionUpdateRequest {

    private ActionKind kind;
    private String destination;

    /**
     * JSON string – stored as is
     */
    private String payload;
}

