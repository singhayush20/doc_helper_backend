package com.ayushsingh.doc_helper.features.usage_monitoring.service.service_impl;

import com.ayushsingh.doc_helper.features.usage_monitoring.config.PricingConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.TokenUsageDto;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenQuotaRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.repository.UserTokenUsageRepository;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.UsageRecordingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageRecordingServiceImpl implements UsageRecordingService {

    private final UserTokenUsageRepository usageRepository;
    private final UserTokenQuotaRepository quotaRepository;
    private final PricingConfig pricingConfig;

    @Transactional
    @Override
    public void recordTokenUsage(TokenUsageDto usageDTO) {
        Long userId = usageDTO.getUserId();
        Long tokensToUse = usageDTO.getTotalTokens();

        log.debug("Recording token usage for userId: {}, tokens: {}", userId, tokensToUse);

        Instant now = Instant.now();

        int updatedRows = quotaRepository.incrementUsageIfActiveAndInPeriod(userId, tokensToUse, now);
        if (updatedRows == 0) {
            log.warn("Failed to increment quota row for userId={}, tokens={}. " +
                    "Row may be inactive or out of billing period. " +
                    "Usage will still be logged.",
                    userId, tokensToUse);
        }

        BigDecimal cost = usageDTO.getEstimatedCost();
        if (cost == null) {
            cost = calculateCost(
                    usageDTO.getModelName(),
                    usageDTO.getPromptTokens(),
                    usageDTO.getCompletionTokens());
        }

        UserTokenUsage usage = mapToUsageEntity(usageDTO, cost);
        usageRepository.save(usage);

        log.debug("Token usage recorded: userId={}, totalTokens={}, cost={}",
                userId, usage.getTotalTokens(), cost);
    }

    private UserTokenUsage mapToUsageEntity(TokenUsageDto dto, BigDecimal cost) {
        return UserTokenUsage.builder()
                .userId(dto.getUserId())
                .documentId(dto.getDocumentId())
                .threadId(dto.getThreadId())
                .messageId(dto.getMessageId())
                .promptTokens(dto.getPromptTokens())
                .completionTokens(dto.getCompletionTokens())
                .totalTokens(dto.getTotalTokens())
                .modelName(dto.getModelName())
                .operationType(dto.getOperationType())
                .estimatedCost(cost)
                .durationMs(dto.getDurationMs())
                .build();
    }

    private BigDecimal calculateCost(String modelName,
            Long promptTokens,
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
}
