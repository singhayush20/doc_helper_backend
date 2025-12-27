package com.ayushsingh.doc_helper.features.usage_monitoring.dto;

import java.math.BigDecimal;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.ChatOperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageDto {
    private Long userId;
    private Long documentId;
    private String threadId;
    private String messageId;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private String modelName;
    private ChatOperationType operationType;
    private BigDecimal estimatedCost;
    private Long durationMs;
}
