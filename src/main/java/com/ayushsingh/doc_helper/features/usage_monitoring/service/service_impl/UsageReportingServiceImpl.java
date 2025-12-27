package com.ayushsingh.doc_helper.features.usage_monitoring.service.service_impl;

import com.ayushsingh.doc_helper.features.usage_monitoring.dto.*;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.ChatOperationType;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;
import com.ayushsingh.doc_helper.features.usage_monitoring.projection.DailyUsageSummaryProjection;
import com.ayushsingh.doc_helper.features.usage_monitoring.projection.UsageBreakdownProjection;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenQuotaRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenUsageRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.UsageReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageReportingServiceImpl implements UsageReportingService {

    private final UserTokenQuotaRepository quotaRepository;
    private final UserTokenUsageRepository usageRepository;

    @Transactional(readOnly = true)
    @Override
    public QuotaInfoResponse getUserQuotaInfo(Long userId) {
        var quota = quotaRepository.findByUserId(userId).orElse(null);
        if (quota == null) {
            return QuotaInfoResponse.builder()
                    .userId(userId)
                    .monthlyLimit(0L)
                    .currentMonthlyUsage(0L)
                    .remainingTokens(0L)
                    .usagePercentage(0.0)
                    .tier(null)
                    .isActive(false)
                    .resetDate(null)
                    .build();
        }

        long remaining = quota.getMonthlyLimit() - quota.getCurrentMonthlyUsage();
        double usagePercentage = quota.getMonthlyLimit() == 0
                ? 0.0
                : (quota.getCurrentMonthlyUsage().doubleValue() /
                        quota.getMonthlyLimit().doubleValue()) * 100.0;

        return QuotaInfoResponse.builder()
                .userId(quota.getUserId())
                .monthlyLimit(quota.getMonthlyLimit())
                .currentMonthlyUsage(quota.getCurrentMonthlyUsage())
                .remainingTokens(remaining)
                .usagePercentage(usagePercentage)
                .resetDate(quota.getResetDate())
                .tier(quota.getTier())
                .isActive(quota.getIsActive())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserTokenUsage> getUserUsageHistory(Long userId, Pageable pageable) {
        return usageRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserTokenUsage> getDocumentUsageHistory(Long userId, Long documentId, Pageable pageable) {
        return usageRepository.findByUserIdAndDocumentIdOrderByCreatedAtDesc(userId, documentId, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public DailyUsageSummaryResponse getDailyUsageSummaryForUser(Long userId, int days) {
        log.debug("Fetching daily usage summary for userId: {} for last {} days", userId, days);

        Instant endDate = Instant.now();
        Instant startDate = endDate.minus(days, ChronoUnit.DAYS);

        List<DailyUsageSummaryProjection> projections = usageRepository.getDailyUsageSummary(userId, startDate,
                endDate);

        List<DailyUsageSummary> dailySummaries = projections.stream()
                .map(this::toDailyUsageSummary)
                .toList();

        UsageTotals totals = calculateTotals(dailySummaries);

        UsageSummaryMeta meta = UsageSummaryMeta.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .totalDays(days)
                .daysWithActivity(dailySummaries.size())
                .totals(totals)
                .build();

        return DailyUsageSummaryResponse.builder()
                .data(dailySummaries)
                .meta(meta)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public BigDecimal getTotalCost(Long userId, Instant startDate) {
        return usageRepository.sumCostForUserSince(userId, startDate);
    }

    @Transactional(readOnly = true)
    @Override
    public UsageBreakdown getUsageBreakdown(Long userId) {
        log.debug("Fetching usage breakdown for userId: {}", userId);

        List<UsageBreakdownProjection> projections = usageRepository.getUsageBreakdownByOperationType(userId);

        return buildUsageBreakdown(projections);
    }

    @Transactional(readOnly = true)
    @Override
    public UsageBreakdown getUsageBreakdownByDateRange(Long userId, Instant startDate, Instant endDate) {
        log.debug("Fetching usage breakdown for userId: {} between {} and {}", userId, startDate, endDate);

        List<UsageBreakdownProjection> projections = usageRepository
                .getUsageBreakdownByOperationTypeAndDateRange(userId, startDate, endDate);

        return buildUsageBreakdown(projections);
    }

    private DailyUsageSummary toDailyUsageSummary(DailyUsageSummaryProjection projection) {
        return new DailyUsageSummary(
                projection.getDate(),
                projection.getTotalTokens(),
                projection.getTotalCost(),
                projection.getRequestCount());
    }

    private UsageTotals calculateTotals(List<DailyUsageSummary> summaries) {
        Long totalTokens = summaries.stream()
                .mapToLong(DailyUsageSummary::getTotalTokens)
                .sum();

        BigDecimal totalCost = summaries.stream()
                .map(DailyUsageSummary::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalRequests = summaries.stream()
                .mapToLong(DailyUsageSummary::getRequestCount)
                .sum();

        return UsageTotals.builder()
                .totalTokens(totalTokens)
                .totalCost(totalCost)
                .totalRequests(totalRequests)
                .build();
    }

    private UsageBreakdown buildUsageBreakdown(List<UsageBreakdownProjection> projections) {
        long chatTokens = 0L;
        long embeddingTokens = 0L;
        BigDecimal chatCost = BigDecimal.ZERO;
        BigDecimal embeddingCost = BigDecimal.ZERO;
        int chatRequestCount = 0;
        int embeddingRequestCount = 0;

        for (UsageBreakdownProjection projection : projections) {
            ChatOperationType operationType = projection.getOperationType();
            int requestCount = projection.getRequestCount().intValue();
            long tokens = projection.getTotalTokens() != null
                    ? projection.getTotalTokens()
                    : 0L;
            BigDecimal cost = projection.getTotalCost() != null
                    ? projection.getTotalCost()
                    : BigDecimal.ZERO;

            if (isChatOperation(operationType)) {
                chatTokens += tokens;
                chatCost = chatCost.add(cost);
                chatRequestCount += requestCount;
            } else if (isEmbeddingOperation(operationType)) {
                embeddingTokens += tokens;
                embeddingCost = embeddingCost.add(cost);
                embeddingRequestCount += requestCount;
            }
        }

        Long totalTokens = chatTokens + embeddingTokens;
        BigDecimal totalCost = chatCost.add(embeddingCost);

        return UsageBreakdown.builder()
                .chatTokens(chatTokens)
                .chatCost(chatCost)
                .embeddingTokens(embeddingTokens)
                .embeddingCost(embeddingCost)
                .totalTokens(totalTokens)
                .totalCost(totalCost)
                .chatRequestCount(chatRequestCount)
                .embeddingRequestCount(embeddingRequestCount)
                .build();
    }

    private boolean isChatOperation(ChatOperationType operationType) {
        if (operationType == null) {
            return false;
        }
        return operationType == ChatOperationType.CHAT_CALL
                || operationType == ChatOperationType.CHAT_STREAM;
    }

    private boolean isEmbeddingOperation(ChatOperationType operationType) {
        if (operationType == null) {
            return false;
        }
        return operationType == ChatOperationType.EMBEDDING_GENERATION;
    }
}
