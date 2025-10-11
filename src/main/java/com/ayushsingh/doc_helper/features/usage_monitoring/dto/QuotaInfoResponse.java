package com.ayushsingh.doc_helper.features.usage_monitoring.dto;

import java.time.Instant;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaInfoResponse {
    private Long userId;
    private Long monthlyLimit;
    private Long currentMonthlyUsage;
    private Long remainingTokens;
    private Double usagePercentage;
    private Instant resetDate;
    private String tier;
    private Boolean isActive;

    public static QuotaInfoResponse fromEntity(
            UserTokenQuota quota) {
        Long remaining = quota.getMonthlyLimit() - quota.getCurrentMonthlyUsage();
        Double percentage = (quota.getCurrentMonthlyUsage().doubleValue() /
                quota.getMonthlyLimit().doubleValue()) * 100;

        return QuotaInfoResponse.builder()
                .userId(quota.getUserId())
                .monthlyLimit(quota.getMonthlyLimit())
                .currentMonthlyUsage(quota.getCurrentMonthlyUsage())
                .remainingTokens(remaining)
                .usagePercentage(percentage)
                .resetDate(quota.getResetDate())
                .tier(quota.getTier())
                .isActive(quota.getIsActive())
                .build();
    }
}
