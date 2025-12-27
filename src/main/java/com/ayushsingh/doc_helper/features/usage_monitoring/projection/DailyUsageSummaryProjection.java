package com.ayushsingh.doc_helper.features.usage_monitoring.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface DailyUsageSummaryProjection {
    Instant getDate();
    Long getTotalTokens();
    BigDecimal getTotalCost();
    Long getRequestCount();
}
