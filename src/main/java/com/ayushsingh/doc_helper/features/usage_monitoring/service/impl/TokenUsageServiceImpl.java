package com.ayushsingh.doc_helper.features.usage_monitoring.service.impl;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.config.security.UserContext;
import com.ayushsingh.doc_helper.features.usage_monitoring.cofig.BillingConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.cofig.PricingConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.*;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.AccountTier;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.ChatOperationType;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;
import com.ayushsingh.doc_helper.features.usage_monitoring.projection.DailyUsageSummaryProjection;
import com.ayushsingh.doc_helper.features.usage_monitoring.projection.UsageBreakdownProjection;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenQuotaRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenUsageRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.TokenUsageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
public class TokenUsageServiceImpl implements TokenUsageService {

        private final UserTokenUsageRepository usageRepository;
        private final UserTokenQuotaRepository quotaRepository;
        private final PricingConfig pricingConfig;
        private final BillingConfig billingConfig;

        public TokenUsageServiceImpl(UserTokenUsageRepository usageRepository,
                        UserTokenQuotaRepository quotaRepository,
                        PricingConfig pricingConfig,
                        BillingConfig billingConfig) {
                this.usageRepository = usageRepository;
                this.quotaRepository = quotaRepository;
                this.billingConfig = billingConfig;
                this.pricingConfig = pricingConfig;
        }

        /**
         * Record token usage for a user.
         */
        @Transactional
        @Override
        public void recordTokenUsage(TokenUsageDto usageDTO) {
                Long userId = usageDTO.getUserId();
                Long tokensToUse = usageDTO.getTotalTokens();

                log.debug("Recording token usage for userId: {}, tokens: {}",
                                userId, tokensToUse);

                Instant now = Instant.now();

                int updatedRows = quotaRepository.tryConsumeTokens(userId, tokensToUse, now);
                if (updatedRows == 0) {
                        log.warn("Quota consumption failed for userId={}, tokens={}", userId, tokensToUse);
                        throw new BaseException(
                                        "Monthly token quota exceeded or account inactive",
                                        ExceptionCodes.QUOTA_EXCEEDED);
                }

                BigDecimal cost = usageDTO.getEstimatedCost();
                if (cost == null) {
                        cost = calculateCost(
                                        usageDTO.getModelName(),
                                        usageDTO.getPromptTokens(),
                                        usageDTO.getCompletionTokens());
                }

                UserTokenUsage usage = UserTokenUsage.builder()
                                .userId(userId)
                                .documentId(usageDTO.getDocumentId())
                                .threadId(usageDTO.getThreadId())
                                .messageId(usageDTO.getMessageId())
                                .promptTokens(usageDTO.getPromptTokens())
                                .completionTokens(usageDTO.getCompletionTokens())
                                .totalTokens(tokensToUse)
                                .modelName(usageDTO.getModelName())
                                .operationType(usageDTO.getOperationType())
                                .estimatedCost(cost)
                                .durationMs(usageDTO.getDurationMs())
                                .build();

                usageRepository.save(usage);

                log.debug("Token usage recorded: userId={}, totalTokens={}, cost={}",
                                userId, usage.getTotalTokens(), cost);
        }

        /**
         * Soft pre-check used by ChatService before sending to LLM.
         * Read-only; does not mutate quota.
         */
        @Transactional(readOnly = true)
        @Override
        public void checkAndEnforceQuota(Long userId, Long tokensToUse) {
                UserTokenQuota quota = getCurrentUserQuota(userId);

                if (!quota.getIsActive()) {
                        log.warn("Inactive user attempted to use tokens: userId={}", userId);
                        throw new BaseException(
                                        "User account is inactive",
                                        ExceptionCodes.USER_QUOTA_INACTIVE);
                }

                long remaining = quota.getMonthlyLimit() - quota.getCurrentMonthlyUsage();
                if (remaining < tokensToUse) {
                        log.warn(
                                        "Insufficient quota to start request: userId={}, remaining={}, required={}",
                                        userId, remaining, tokensToUse);
                        throw new BaseException(
                                        "Insufficient token quota to start this operation.",
                                        ExceptionCodes.QUOTA_EXCEEDED);
                }

                log.debug("Soft quota pre-check passed for userId: {}, remaining: {}, required: {}",
                                userId, remaining, tokensToUse);
        }

        /**
         * Update user's token quota.
         * WARNING: this is intended for admin/manual adjustments only.
         * Normal request-path consumption MUST go through recordTokenUsage
         * (tryConsumeTokens).
         */
        @Transactional
        @Override
        public void updateUserQuota(Long userId, Long tokensUsed) {
                log.warn("updateUserQuota called for userId={} with tokensUsed={}. " +
                                "This should only be used for admin/manual adjustments.", userId, tokensUsed);
                quotaRepository.incrementUsage(userId, tokensUsed);
        }

        /**
         * Get current user quota or throw if missing.
         */
        @Override
        public UserTokenQuota getCurrentUserQuota(Long userId) {
                return quotaRepository.findByUserId(userId)
                                .orElseThrow(() -> new BaseException(
                                                "No quota info found for user: " + userId,
                                                ExceptionCodes.QUOTA_NOT_FOUND));
        }

        /**
         * Create default quota for new user.
         * Called on user onboarding.
         */
        @Transactional
        public UserTokenQuota createDefaultQuota(Long userId) {
                log.debug("Creating default quota for userId: {}", userId);

                Instant resetDate = getNextMonthStart();

                UserTokenQuota quota = UserTokenQuota.builder()
                                .userId(userId)
                                .monthlyLimit(billingConfig.getDefaultMonthlyLimit())
                                .currentMonthlyUsage(0L)
                                .resetDate(resetDate)
                                .tier(AccountTier.FREE)
                                .isActive(true)
                                .build();

                return quotaRepository.save(quota);
        }

        /**
         * Reset user quota for a new billing period.
         * Used only by QuotaResetScheduler.
         */
        @Transactional
        @Override
        public void resetQuota(UserTokenQuota quota) {
                Long userId = quota.getUserId();
                log.debug("Resetting quota for userId: {}", userId);

                Instant now = Instant.now();
                Instant newResetDate = getNextMonthStart();

                int updated = quotaRepository.resetQuota(userId, now, newResetDate);
                if (updated == 0) {
                        log.debug("Quota already reset or not due for userId={}", userId);
                } else {
                        log.info("Quota reset for userId={}", userId);
                }
        }

        /**
         * Get start of next month in configured billing timezone.
         */
        private Instant getNextMonthStart() {
                ZoneId zoneId = ZoneId.of(billingConfig.getBillingTimezone());
                YearMonth nextMonth = YearMonth.now(zoneId).plusMonths(1);
                ZonedDateTime firstDayNextMonth = nextMonth.atDay(1)
                                .atStartOfDay(zoneId);
                return firstDayNextMonth.toInstant();
        }

        /**
         * Calculate cost based on token usage.
         */
        private BigDecimal calculateCost(String modelName, Long promptTokens,
                        Long completionTokens) {
                BigDecimal inputCostPer1k = pricingConfig.getInputCost(modelName);
                BigDecimal outputCostPer1k = pricingConfig.getOutputCost(modelName);

                BigDecimal inputCost = BigDecimal.valueOf(promptTokens)
                                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                                .multiply(inputCostPer1k);

                BigDecimal outputCost = BigDecimal.valueOf(completionTokens)
                                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                                .multiply(outputCostPer1k);

                return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
        }

        /**
         * Get current month usage for user.
         * Read-only.
         */
        @Transactional(readOnly = true)
        @Override
        public Long getCurrentMonthUsage(Long userId) {
                UserTokenQuota quota = getCurrentUserQuota(userId);
                return quota.getCurrentMonthlyUsage();
        }

        /**
         * Get user quota information for UI.
         */
        @Transactional(readOnly = true)
        @Override
        public QuotaInfoResponse getUserQuotaInfo(Long userId) {
                UserTokenQuota quota = getCurrentUserQuota(userId);

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

        /**
         * Get usage history with pagination.
         */
        @Transactional(readOnly = true)
        @Override
        public Page<UserTokenUsage> getUserUsageHistory(Pageable pageable) {
                Long userId = UserContext.getCurrentUser().getUser().getId();
                return usageRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        /**
         * Get usage for specific document.
         */
        @Transactional(readOnly = true)
        @Override
        public Page<UserTokenUsage> getDocumentUsageHistory(Long documentId,
                        Pageable pageable) {
                Long userId = UserContext.getCurrentUser().getUser().getId();

                return usageRepository.findByUserIdAndDocumentIdOrderByCreatedAtDesc(
                                userId, documentId, pageable);
        }

        /**
         * Get daily usage summary for the last N days.
         */
        @Transactional(readOnly = true)
        @Override
        public DailyUsageSummaryResponse getDailyUsageSummaryForDays(int days) {
                Long userId = UserContext.getCurrentUser().getUser().getId();

                log.debug("Fetching daily usage summary for userId: {} for last {} days",
                                userId, days);

                Instant endDate = Instant.now();
                Instant startDate = endDate.minus(days, ChronoUnit.DAYS);

                List<DailyUsageSummaryProjection> projections = usageRepository
                                .getDailyUsageSummary(userId, startDate, endDate);

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

        private DailyUsageSummary toDailyUsageSummary(
                        DailyUsageSummaryProjection projection) {
                return new DailyUsageSummary(
                                projection.getDate(),
                                projection.getTotalTokens(),
                                projection.getTotalCost(),
                                projection.getRequestCount());
        }

        /**
         * Calculate totals from daily summaries.
         */
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

        /**
         * Get total cost for user in date range.
         */
        @Transactional(readOnly = true)
        @Override
        public BigDecimal getTotalCost(Long userId, Instant startDate) {
                return usageRepository.sumCostForUserSince(userId, startDate);
        }

        /**
         * Update user tier (for upgrades/downgrades).
         */
        @Transactional
        @Override
        public void updateUserTier(Long userId, String newTier, Long newLimit) {
                // TODO: Design account upgradation logic
                log.error("Account upgradation not implemented!");

                throw new BaseException(
                                "Account upgradation is not supported at the moment!",
                                ExceptionCodes.ACCOUNT_UPGRADATION_FAILURE);
        }

        /**
         * Get usage breakdown by operation type (chat vs embedding).
         */
        @Transactional(readOnly = true)
        @Override
        public UsageBreakdown getUsageBreakdown(Long userId) {
                log.debug("Fetching usage breakdown for userId: {}", userId);

                List<UsageBreakdownProjection> projections = usageRepository
                                .getUsageBreakdownByOperationType(userId);

                return buildUsageBreakdown(projections);
        }

        /**
         * Get usage breakdown for a specific date range.
         */
        @Transactional(readOnly = true)
        @Override
        public UsageBreakdown getUsageBreakdownByDateRange(Long userId,
                        Instant startDate,
                        Instant endDate) {
                log.debug("Fetching usage breakdown for userId: {} between {} and {}",
                                userId, startDate, endDate);

                List<UsageBreakdownProjection> projections = usageRepository
                                .getUsageBreakdownByOperationTypeAndDateRange(
                                                userId, startDate, endDate);

                return buildUsageBreakdown(projections);
        }

        private UsageBreakdown buildUsageBreakdown(
                        List<UsageBreakdownProjection> projections) {
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
                return operationType == ChatOperationType.CHAT_CALL ||
                                operationType == ChatOperationType.CHAT_STREAM;
        }

        private boolean isEmbeddingOperation(ChatOperationType operationType) {
                if (operationType == null) {
                        return false;
                }
                return operationType == ChatOperationType.EMBEDDING_GENERATION;
        }
}
