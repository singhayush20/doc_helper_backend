package com.ayushsingh.doc_helper.features.usage_monitoring.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.AccountTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.usage_monitoring.cofig.BillingConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.cofig.PricingConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.DailyUsageSummary;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.QuotaInfoResponse;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.TokenUsageDto;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.UsageBreakdown;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenQuotaRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenUsageRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.TokenUsageService;

import lombok.extern.slf4j.Slf4j;

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
         * Record token usage for a user
         */
        @Transactional
        @Override
        public void recordTokenUsage(TokenUsageDto usageDTO) {
                log.debug("Recording token usage for userId: {}, tokens: {}",
                                usageDTO.getUserId(), usageDTO.getTotalTokens());

                // Check if user has exceeded quota
                checkAndEnforceQuota(usageDTO.getUserId(), usageDTO.getTotalTokens());

                // Calculate estimated cost if not provided
                BigDecimal cost = usageDTO.getEstimatedCost();
                if (cost == null) {
                        cost = calculateCost(
                                        usageDTO.getModelName(),
                                        usageDTO.getPromptTokens(),
                                        usageDTO.getCompletionTokens());
                }

                // Create usage record
                UserTokenUsage usage = UserTokenUsage.builder()
                                .userId(usageDTO.getUserId())
                                .documentId(usageDTO.getDocumentId())
                                .threadId(usageDTO.getThreadId())
                                .messageId(usageDTO.getMessageId())
                                .promptTokens(usageDTO.getPromptTokens())
                                .completionTokens(usageDTO.getCompletionTokens())
                                .totalTokens(usageDTO.getTotalTokens())
                                .modelName(usageDTO.getModelName())
                                .operationType(usageDTO.getOperationType())
                                .estimatedCost(cost)
                                .durationMs(usageDTO.getDurationMs())
                                .build();

                usageRepository.save(usage);

                updateUserQuota(usageDTO.getUserId(), usageDTO.getTotalTokens());

                log.debug("Token usage recorded: userId={}, totalTokens={}, " +
                      "cost={}",
                                usageDTO.getUserId(), usage.getTotalTokens(), cost);
        }

        /**
         * Check if user has quota available
         */
        @Override
        @Transactional
        public void checkAndEnforceQuota(Long userId, Long tokensToUse) {
                UserTokenQuota quota = getCurrentUserQuota(userId);

                // Reset quota if needed
                if (Instant.now().isAfter(quota.getResetDate())) {
                        resetQuota(quota);
                }

                if (!quota.getIsActive()) {
                        log.warn("Inactive user attempted to use tokens: userId={}", userId);
                        throw new BaseException(
                                        "User account is inactive",
                                        ExceptionCodes.USER_QUOTA_INACTIVE);
                }

                Long newUsage = quota.getCurrentMonthlyUsage() + tokensToUse;
                if (newUsage > quota.getMonthlyLimit()) {
                        log.warn("Token quota exceeded for userId: {}, current: {}, limit: {}",
                                        userId, newUsage, quota.getMonthlyLimit());
                        throw new BaseException(
                                        String.format("Monthly token quota exceeded. Used: %d, Limit: %d",
                                                        newUsage, quota.getMonthlyLimit()),
                                        ExceptionCodes.QUOTA_EXCEEDED);
                }

                log.debug("Quota check passed for userId: {}, newUsage: {}/{}",
                                userId, newUsage, quota.getMonthlyLimit());
        }

        /**
         * Update user's token quota
         */
        @Transactional
        @Override
        public void updateUserQuota(Long userId, Long tokensUsed) {
                quotaRepository.incrementUsage(userId, tokensUsed);
        }

        /**
         * Get or create user quota
         */
        @Override
        public UserTokenQuota getCurrentUserQuota(Long userId) {
                return quotaRepository.findByUserId(userId)
                        .orElseThrow(()-> new BaseException("No quota info " +
                                                            "found for user: " +
                                                            userId,
                                ExceptionCodes.QUOTA_NOT_FOUND));
        }

        /**
         * Create default quota for new user
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
         * Reset user quota (called when month changes)
         */
        @Override
        @Transactional
        public void resetQuota(UserTokenQuota quota) {
                log.debug("Resetting quota for userId: {}", quota.getUserId());
                Instant newResetDate = getNextMonthStart();
                quotaRepository.resetQuota(quota.getUserId(), newResetDate);
        }

        /**
         * Get start of next month in IST
         */
        private Instant getNextMonthStart() {
                // TODO: Check if UTC must be used here instead of IST
                ZoneId zoneId = ZoneId.of(billingConfig.getBillingTimezone());
                YearMonth nextMonth = YearMonth.now(zoneId).plusMonths(1);
                ZonedDateTime firstDayNextMonth = nextMonth.atDay(1).atStartOfDay(zoneId);
                return firstDayNextMonth.toInstant();
        }

        /**
         * Calculate cost based on token usage
         */
        private BigDecimal calculateCost(String modelName, Long promptTokens, Long completionTokens) {
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
         * Get current month usage for user
         */
        @Override
        public Long getCurrentMonthUsage(Long userId) {
                UserTokenQuota quota = getCurrentUserQuota(userId);
                return quota.getCurrentMonthlyUsage();
        }

        /**
         * Get user quota information
         */
        @Override
        public QuotaInfoResponse getUserQuotaInfo(Long userId) {
                // TODO: Revisit this implementation - use projection
                UserTokenQuota quota = getCurrentUserQuota(userId);
                return QuotaInfoResponse.fromEntity(quota);
        }

        /**
         * Get usage history with pagination
         */
        @Override
        public Page<UserTokenUsage> getUserUsageHistory(Long userId, Pageable pageable) {
                // TODO: Revisit this implementation
                return usageRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        }

        /**
         * Get usage for specific document
         */
        @Override
        public Page<UserTokenUsage> getDocumentUsageHistory(
                        Long userId, Long documentId, Pageable pageable) {
                // TODO: Revisit this implementation
                return usageRepository.findByUserIdAndDocumentIdOrderByTimestampDesc(
                                userId, documentId, pageable);
        }

        /**
         * Get daily usage summary
         */
        @Override
        public List<DailyUsageSummary> getDailyUsageSummary(
                        Long userId, Instant startDate, Instant endDate) {
                // TODO: Revisit this implementation
                List<Object[]> results = usageRepository.getDailyUsageSummary(
                                userId, startDate, endDate);

                return results.stream()
                                .map(row -> new DailyUsageSummary(
                                                ((java.sql.Date) row[0]).toLocalDate(),
                                                ((Number) row[1]).longValue(),
                                                (BigDecimal) row[2],
                                                ((Number) row[3]).longValue()))
                                .toList();
        }

        /**
         * Get total cost for user in date range
         */
        @Override
        public BigDecimal getTotalCost(Long userId, Instant startDate) {
                return usageRepository.sumCostForUserSince(userId, startDate);
        }

        /**
         * Update user tier (for upgrades/downgrades)
         */
        @Transactional
        @Override
        public void updateUserTier(Long userId, String newTier, Long newLimit) {
                // TODO: Revisit this implementation- What if tier is changed
                //  from paid to free
                UserTokenQuota quota = getCurrentUserQuota(userId);
                quota.setTier(AccountTier.valueOf(newTier));
                quota.setMonthlyLimit(newLimit);
                quota.setUpdatedAt(Instant.now());
                quotaRepository.save(quota);

                log.debug("Updated user tier: userId={}, tier={}, limit={}",
                                userId, newTier, newLimit);
        }

        /**
         * Get usage breakdown by operation type (chat vs embedding)
         */
        @Override
        public UsageBreakdown getUsageBreakdown(Long userId) {
                log.debug("Fetching usage breakdown for userId: {}", userId);
                // TODO: Use projection
                List<Object[]> results = usageRepository.getUsageBreakdownByOperationType(userId);

                return buildUsageBreakdown(results);
        }

        /**
         * Get usage breakdown for a specific date range
         */
        @Override
        public UsageBreakdown getUsageBreakdownByDateRange(
                        Long userId,
                        Instant startDate,
                        Instant endDate) {
                log.debug("Fetching usage breakdown for userId: {} between {} and {}",
                                userId, startDate, endDate);
                // TODO: Add projection
                List<Object[]> results = usageRepository.getUsageBreakdownByOperationTypeAndDateRange(
                                userId, startDate, endDate);

                return buildUsageBreakdown(results);
        }

        /**
         * Build UsageBreakdown DTO from query results
         */
        private UsageBreakdown buildUsageBreakdown(List<Object[]> results) {
                long chatTokens = 0L;
                long embeddingTokens = 0L;
                BigDecimal chatCost = BigDecimal.ZERO;
                BigDecimal embeddingCost = BigDecimal.ZERO;
                int chatRequestCount = 0;
                int embeddingRequestCount = 0;

                for (Object[] row : results) {
                        String operationType = (String) row[0];
                        int requestCount = ((Number) row[1]).intValue();
                        long tokens = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                        BigDecimal cost = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;

                        // Categorize by operation type
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

        /**
         * Check if operation type is a chat operation
         */
        private boolean isChatOperation(String operationType) {
                if (operationType == null) {
                        return false;
                }
                String lowerCaseOp = operationType.toLowerCase();
                return lowerCaseOp.contains("chat") ||
                                lowerCaseOp.equals("stream") ||
                                lowerCaseOp.equals("call");
        }

        /**
         * Check if operation type is an embedding operation
         */
        private boolean isEmbeddingOperation(String operationType) {
                if (operationType == null) {
                        return false;
                }
                return operationType.equalsIgnoreCase("embedding");
        }
}
