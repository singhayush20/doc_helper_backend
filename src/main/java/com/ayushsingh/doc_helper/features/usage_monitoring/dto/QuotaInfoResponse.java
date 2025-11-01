package com.ayushsingh.doc_helper.features.usage_monitoring.dto;

import java.time.Instant;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.AccountTier;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class QuotaInfoResponse {
    private Long userId;
    private Long monthlyLimit;
    private Long currentMonthlyUsage;
    private Long remainingTokens;
    private Double usagePercentage;
    private Instant resetDate;
    private AccountTier tier;
    private Boolean isActive;
}
