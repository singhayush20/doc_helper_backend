package com.ayushsingh.doc_helper.features.product_features.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UsageQuotaDto {

    private String metric;   // TOKENS, PAGES, REQUESTS
    private Long used;
    private Long limit;

    /**
     * Optional – frontend may render reset info
     */
    private Long resetAtEpochMillis;
}
