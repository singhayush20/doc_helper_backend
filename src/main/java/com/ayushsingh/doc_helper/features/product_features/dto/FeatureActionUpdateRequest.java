package com.ayushsingh.doc_helper.features.product_features.dto;

import com.ayushsingh.doc_helper.features.product_features.entity.ActionKind;
import lombok.Data;

@Data
public class FeatureActionUpdateRequest {

    private ActionKind kind;
    private String destination;

    /**
     * JSON string – stored as is
     */
    private String payload;
}

