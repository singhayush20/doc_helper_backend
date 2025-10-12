package com.ayushsingh.doc_helper.features.usage_monitoring.projection;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.ChatOperationType;

import java.math.BigDecimal;

public interface UsageBreakdownProjection {
    ChatOperationType getOperationType();
    Long getRequestCount();
    Long getTotalTokens();
    BigDecimal getTotalCost();
}
