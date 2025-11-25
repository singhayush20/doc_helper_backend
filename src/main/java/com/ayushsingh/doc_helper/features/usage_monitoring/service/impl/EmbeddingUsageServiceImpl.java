package com.ayushsingh.doc_helper.features.usage_monitoring.service.impl;

import com.ayushsingh.doc_helper.features.usage_monitoring.dto.TokenUsageDto;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.ChatOperationType;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.EmbeddingUsageService;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.UsageRecordingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingUsageServiceImpl implements EmbeddingUsageService {

        private final UsageRecordingService tokenUsageService;

        /**
         * Track embedding generation for document upload.
         */
        @Transactional
        @Override
        public void recordEmbeddingUsage(Long userId,
                        Long documentId,
                        String embeddingModel,
                        Integer numChunks,
                        Long totalTokens) {

                log.debug(
                                "Recording embedding usage: userId={}, documentId={}, chunks={}, tokens={}",
                                userId, documentId, numChunks, totalTokens);

                // Build DTO and delegate to common token usage path
                TokenUsageDto usageDto = TokenUsageDto.builder()
                                .userId(userId)
                                .documentId(documentId)
                                .threadId(null)
                                .messageId(null)
                                .promptTokens(totalTokens) // all embedding tokens as input
                                .completionTokens(0L)
                                .totalTokens(totalTokens)
                                .modelName(embeddingModel)
                                .operationType(ChatOperationType.EMBEDDING_GENERATION)
                                .durationMs(0L)
                                .build();

                tokenUsageService.recordTokenUsage(usageDto);

                log.info(
                                "Embedding usage recorded via TokenUsageService: userId={}, documentId={}, chunks={}, tokens={}",
                                userId, documentId, numChunks, totalTokens);
        }
}
