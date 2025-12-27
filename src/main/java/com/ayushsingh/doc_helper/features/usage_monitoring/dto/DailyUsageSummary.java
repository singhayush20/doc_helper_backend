package com.ayushsingh.doc_helper.features.usage_monitoring.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyUsageSummary {
    private Instant date;
    private Long totalTokens;
    private BigDecimal totalCost;
    private Long requestCount;
}
