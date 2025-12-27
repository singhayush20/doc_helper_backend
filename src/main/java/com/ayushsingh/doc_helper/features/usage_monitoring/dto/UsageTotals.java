package com.ayushsingh.doc_helper.features.usage_monitoring.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageTotals {
    private Long totalTokens;
    private java.math.BigDecimal totalCost;
    private Long totalRequests;
}
