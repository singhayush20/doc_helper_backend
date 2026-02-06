package com.ayushsingh.doc_helper.features.product_features.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeatureCreateRequestDto {

    private String code;        // OCR, DOC_CHAT, etc (immutable)
    private String name;
    private String description;
    private String type;        // LLM, OCR, AGENT
    private String usageMetric; // TOKEN_COUNT, etc
}
