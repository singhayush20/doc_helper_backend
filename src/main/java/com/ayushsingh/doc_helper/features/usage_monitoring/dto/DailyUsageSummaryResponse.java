package com.ayushsingh.doc_helper.features.usage_monitoring.dto;

import java.time.Instant;
import java.util.List;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DailyUsageSummaryResponse {
    private List<DailyUsageSummary> data;
    private UsageSummaryMeta meta;
}
