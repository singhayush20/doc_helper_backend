package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryContentDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateRequestDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryListResponseDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryResponseDto;
import com.ayushsingh.doc_helper.features.doc_summary.entity.Document;
import com.ayushsingh.doc_helper.features.doc_summary.entity.DocumentChunk;
import com.ayushsingh.doc_helper.features.doc_summary.entity.DocumentSummary;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryTone;
import com.ayushsingh.doc_helper.features.doc_summary.repository.DocumentChunkRepository;
import com.ayushsingh.doc_helper.features.doc_summary.repository.DocumentRepository;
import com.ayushsingh.doc_helper.features.doc_summary.repository.DocumentSummaryRepository;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentChunkingService;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentService;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentSummaryService;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryGenerationResult;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryGenerationService;
import com.ayushsingh.doc_helper.features.doc_util.DocService;
import com.ayushsingh.doc_helper.features.doc_util.service_impl.DocumentParsingService;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.ayushsingh.doc_helper.features.product_features.entity.UsageMetric;
import com.ayushsingh.doc_helper.features.product_features.service.UsageQuotaService;
import com.ayushsingh.doc_helper.features.user_activity.dto.ActivityTarget;
import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityType;
import com.ayushsingh.doc_helper.features.user_activity.service.UserActivityRecorder;
import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.user_doc.entity.DocumentStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentSummaryServiceImpl implements DocumentSummaryService {

    private final DocumentService documentService;
    private final DocumentSummaryRepository documentSummaryRepository;
    private final UsageQuotaService usageQuotaService;
    private final SummaryGenerationService summaryGenerationService;
    private final DocService docService;
    private final DocumentParsingService documentParsingService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentChunkingService summaryChunker;
    private final ModelMapper modelMapper;
    private final UserActivityRecorder userActivityRecorder;

    @Transactional
    @Override
    public SummaryResponseDto createSummary(
            SummaryCreateRequestDto requestDto) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        Long documentId = requestDto.getDocumentId();

        Document document = documentService.getByIdForUser(documentId, userId);
        List<String> chunks = loadOrCreateChunks(document);
        if (chunks.isEmpty()) {
            throw new BaseException(
                    "Document content is empty",
                    ExceptionCodes.DOCUMENT_PARSING_FAILED);
        }

        SummaryTone tone = parseTone(requestDto.getTone());
        SummaryLength length = parseLength(requestDto.getLength());

        long remainingTokens = usageQuotaService
                .getRemainingTokens(userId, FeatureCodes.DOC_SUMMARY.name());

        SummaryGenerationResult result = summaryGenerationService.generate(
                chunks,
                tone,
                length,
                remainingTokens,
                tokens -> usageQuotaService.consume(
                        userId,
                        FeatureCodes.DOC_SUMMARY.name(),
                        UsageMetric.TOKEN_COUNT,
                        tokens));

        Integer nextVersion = resolveNextVersion(documentId);

        DocumentSummary summary = new DocumentSummary();
        summary.setDocumentId(documentId);
        summary.setVersionNumber(nextVersion);
        summary.setTone(tone);
        summary.setWordCount(result.content().wordCount());
        summary.setLength(length);
        summary.setContent(result.content().summary());
        summary.setTokensUsed(result.tokensUsed());

        DocumentSummary saved = documentSummaryRepository.save(summary);

        userActivityRecorder.record(userId, new ActivityTarget(ActivityTargetType.SUMMARY_DOCUMENT, documentId), UserActivityType.DOCUMENT_SUMMARY);

        return SummaryResponseDto.builder()
                .summaryId(saved.getId())
                .createdAt(saved.getCreatedAt())
                .tone(saved.getTone())
                .length(saved.getLength())
                .version(saved.getVersionNumber())
                .tokensUsed(saved.getTokensUsed())
                .content(saved.getContent())
                .wordCount(saved.getWordCount())
                .build();
    }

    @Override
    public SummaryListResponseDto getSummaries(Long documentId) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        documentService.getByIdForUser(documentId, userId);

        return new SummaryListResponseDto(
                documentSummaryRepository
                        .findByDocumentIdOrderByVersionNumberAsc(documentId)
                        .stream()
                        .map(s -> modelMapper.map(s, SummaryResponseDto.class))
                        .toList());
    }

    @Override
    public SummaryContentDto getSummary(Long summaryId) {
        DocumentSummary summary = documentSummaryRepository.findById(summaryId)
                .orElseThrow(() -> new BaseException(
                        "Summary not found",
                        ExceptionCodes.DOCUMENT_NOT_FOUND));

        Long userId = UserContext.getCurrentUser().getUser().getId();
        var isDocumentPresent = documentService.existsByIdAndUserId(summary.getDocumentId(), userId);

        if (isDocumentPresent) {
            return modelMapper.map(summary, SummaryContentDto.class);
        } else {
            throw new BaseException(
                    "DOcument not found",
                    ExceptionCodes.DOCUMENT_NOT_FOUND);
        }
    }

    private Integer resolveNextVersion(Long documentId) {
        Integer max = documentSummaryRepository.findMaxVersionNumber(documentId);
        return max == null ? 1 : max + 1;
    }

    private SummaryTone parseTone(String tone) {
        try {
            return SummaryTone.valueOf(tone);
        } catch (Exception e) {
            throw new BaseException(
                    "Invalid tone value",
                    ExceptionCodes.INVALID_FEATURE_CONFIG);
        }
    }

    private SummaryLength parseLength(String length) {
        try {
            return SummaryLength.valueOf(length);
        } catch (Exception e) {
            throw new BaseException(
                    "Invalid length value",
                    ExceptionCodes.INVALID_FEATURE_CONFIG);
        }
    }

    private List<String> loadOrCreateChunks(Document document) {
        List<DocumentChunk> existing = documentChunkRepository
                .findByDocumentIdOrderByChunkIndexAsc(document.getId());
        if (!existing.isEmpty()) {
            if (document.getStatus() != DocumentStatus.READY) {
                document.setStatus(DocumentStatus.READY);
                documentRepository.save(document);
            }
            return existing.stream()
                    .map(DocumentChunk::getContentText)
                    .toList();
        }

        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        try {
            String text = extractText(document);
            if (text == null || text.isBlank()) {
                throw new BaseException(
                        "Failed to parse document",
                        ExceptionCodes.DOCUMENT_PARSING_FAILED);
            }

            List<String> chunks = summaryChunker.splitWithOverlap(text);
            if (chunks.isEmpty()) {
                throw new BaseException(
                        "Failed to parse document",
                        ExceptionCodes.DOCUMENT_PARSING_FAILED);
            }

            List<DocumentChunk> chunksList = new ArrayList<>(chunks.size());
            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(chunkIndex);
                chunk.setContentText(chunks.get(chunkIndex));
                chunksList.add(chunk);
            }

            documentChunkRepository.saveAll(chunksList);
            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);
            return chunks;
        } catch (BaseException e) {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw e;
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw new BaseException(
                    "Failed to parse document",
                    ExceptionCodes.DOCUMENT_PARSING_FAILED);
        }
    }

    private String extractText(Document document) {
        Resource resource = docService.loadFileAsResource(document.getFileName());
        return documentParsingService.extractText(resource, document.getOriginalFilename());
    }

    @Override
    public void deleteSummaryByDocumentId(Long documentId) {
        List<DocumentSummary> summaries = documentSummaryRepository.findByDocumentIdOrderByVersionNumberAsc(documentId);
        documentSummaryRepository.deleteAll(summaries);
    }
}
