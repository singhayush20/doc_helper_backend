package com.ayushsingh.doc_helper.features.usage_monitoring.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class UsageBreakdown {
    private Long chatTokens;
    private BigDecimal chatCost;
    private Long embeddingTokens;
    private BigDecimal embeddingCost;
    private Long totalTokens;
    private BigDecimal totalCost;
    private Integer chatRequestCount;
    private Integer embeddingRequestCount;
}