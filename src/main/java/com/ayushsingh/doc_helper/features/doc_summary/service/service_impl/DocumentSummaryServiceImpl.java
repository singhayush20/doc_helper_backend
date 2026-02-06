package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryContentDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateRequestDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateResponseDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryMetadataDto;
import com.ayushsingh.doc_helper.features.doc_summary.entity.Document;
import com.ayushsingh.doc_helper.features.doc_summary.entity.DocumentSummary;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryTone;
import com.ayushsingh.doc_helper.features.doc_summary.repository.DocumentSummaryRepository;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentService;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentSummaryService;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryGenerationResult;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryGenerationService;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.ayushsingh.doc_helper.features.product_features.entity.UsageMetric;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureAccessService;
import com.ayushsingh.doc_helper.features.product_features.service.UsageQuotaService;
import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentSummaryServiceImpl implements DocumentSummaryService {

    private final DocumentService documentService;
    private final DocumentSummaryRepository documentSummaryRepository;
    private final FeatureAccessService featureAccessService;
    private final UsageQuotaService usageQuotaService;
    private final SummaryGenerationService summaryGenerationService;

    @Transactional
    @Override
    public SummaryCreateResponseDto createSummary(
            Long documentId,
            SummaryCreateRequestDto request
    ) {
        Long userId = UserContext.getCurrentUser().getUser().getId();

        Document document = documentService.getByIdForUser(documentId, userId);
        if (document.getContentText() == null || document.getContentText().isBlank()) {
            throw new BaseException(
                    "Document content is empty",
                    ExceptionCodes.DOCUMENT_PARSING_FAILED
            );
        }

        SummaryTone tone = parseTone(request.getTone());
        SummaryLength length = parseLength(request.getLength());

        featureAccessService.assertFeatureAccess(userId, FeatureCodes.DOC_SUMMARY);

        long estimatedTokens = summaryGenerationService.estimateTokens(
                document.getContentText(), length);

        usageQuotaService.assertQuotaAvailable(
                userId,
                FeatureCodes.DOC_SUMMARY,
                estimatedTokens
        );

        SummaryGenerationResult result =
                summaryGenerationService.generate(
                        document.getContentText(), tone, length);

        Integer nextVersion = resolveNextVersion(documentId);

        DocumentSummary summary = new DocumentSummary();
        summary.setDocumentId(documentId);
        summary.setVersionNumber(nextVersion);
        summary.setTone(tone);
        summary.setLength(length);
        summary.setContent(result.content());
        summary.setTokensUsed(result.tokensUsed());

        DocumentSummary saved = documentSummaryRepository.save(summary);

        usageQuotaService.consume(
                userId,
                FeatureCodes.DOC_SUMMARY,
                UsageMetric.TOKEN_COUNT,
                result.tokensUsed()
        );

        return SummaryCreateResponseDto.builder()
                .summaryId(saved.getId())
                .version(saved.getVersionNumber())
                .tokensUsed(saved.getTokensUsed())
                .build();
    }

    @Override
    public List<SummaryMetadataDto> getSummaries(Long documentId) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        documentService.getByIdForUser(documentId, userId);

        return documentSummaryRepository
                .findByDocumentIdOrderByVersionNumberAsc(documentId)
                .stream()
                .map(this::toMetadata)
                .toList();
    }

    @Override
    public SummaryContentDto getSummary(Long summaryId) {
        DocumentSummary summary = documentSummaryRepository.findById(summaryId)
                .orElseThrow(() -> new BaseException(
                        "Summary not found",
                        ExceptionCodes.DOCUMENT_NOT_FOUND
                ));

        Long userId = UserContext.getCurrentUser().getUser().getId();
        documentService.getByIdForUser(summary.getDocumentId(), userId);

        return SummaryContentDto.builder()
                .summaryId(summary.getId())
                .documentId(summary.getDocumentId())
                .version(summary.getVersionNumber())
                .tone(summary.getTone())
                .length(summary.getLength())
                .content(summary.getContent())
                .tokensUsed(summary.getTokensUsed())
                .createdAt(summary.getCreatedAt())
                .build();
    }

    private Integer resolveNextVersion(Long documentId) {
        Integer max = documentSummaryRepository.findMaxVersionNumber(documentId);
        return max == null ? 1 : max + 1;
    }

    private SummaryMetadataDto toMetadata(DocumentSummary summary) {
        return SummaryMetadataDto.builder()
                .summaryId(summary.getId())
                .version(summary.getVersionNumber())
                .tone(summary.getTone())
                .length(summary.getLength())
                .tokensUsed(summary.getTokensUsed())
                .createdAt(summary.getCreatedAt())
                .build();
    }

    private SummaryTone parseTone(String tone) {
        try {
            return SummaryTone.valueOf(tone);
        } catch (Exception e) {
            throw new BaseException(
                    "Invalid tone value",
                    ExceptionCodes.INVALID_FEATURE_CONFIG
            );
        }
    }

    private SummaryLength parseLength(String length) {
        try {
            return SummaryLength.valueOf(length);
        } catch (Exception e) {
            throw new BaseException(
                    "Invalid length value",
                    ExceptionCodes.INVALID_FEATURE_CONFIG
            );
        }
    }
}
