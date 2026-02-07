package com.ayushsingh.doc_helper.features.product_features.dto;

import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeatureCreateRequestDto {

    private FeatureCodes code;        // OCR, DOC_CHAT, etc (immutable)
    private String name;
    private String description;
    private String type;        // LLM, OCR, AGENT
    private String usageMetric; // TOKEN_COUNT, etc
}
