package com.ayushsingh.doc_helper.features.product_features.dto;

import com.ayushsingh.doc_helper.features.product_features.entity.ActionKind;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FeatureActionDto {

    private ActionKind kind;

    /**
     * Semantic destination
     * Example: DOC_CHAT_PAGE, OCR_TOOL
     */
    private String destination;

    /**
     * Arbitrary payload passed to frontend
     */
    private Map<String, Object> payload;
}
