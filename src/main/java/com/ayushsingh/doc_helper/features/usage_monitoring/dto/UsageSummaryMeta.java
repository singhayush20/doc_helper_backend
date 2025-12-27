package com.ayushsingh.doc_helper.features.usage_monitoring.dto;

import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UsageSummaryMeta {
    private Long userId;
    private Instant startDate;
    private Instant endDate;
    private Integer totalDays;
    private Integer daysWithActivity;
    private UsageTotals totals;
}
