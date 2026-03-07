package com.ayushsingh.doc_helper.features.product_features.dto;

import java.time.Instant;

import com.ayushsingh.doc_helper.features.product_features.entity.UsageMetric;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UsageQuotaDto {

    private UsageMetric metric;   // TOKENS, PAGES, REQUESTS
    private Long used;
    private Long limit;
    private Instant resetAt;
}
