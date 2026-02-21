package com.ayushsingh.doc_helper.features.doc_summary.controller;

import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryContentDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateRequestDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryListResponseDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryResponseDto;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentSummaryService;
import com.ayushsingh.doc_helper.features.product_features.guard.RequireFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/summarizer")
@RequiredArgsConstructor
public class DocumentSummaryController {

    private final DocumentSummaryService documentSummaryService;

    @PostMapping("/documents")
    @RequireFeature(code = "DOC_SUMMARY")
    public ResponseEntity<SummaryResponseDto> createSummary(
            @RequestBody SummaryCreateRequestDto summaryCreateRequestDto
    ) {
        return ResponseEntity.ok(
                        documentSummaryService.createSummary(
                        summaryCreateRequestDto)
        );
    }

    @GetMapping("/documents/{documentId}")
    @RequireFeature(code = "DOC_SUMMARY")
    public ResponseEntity<SummaryListResponseDto> getSummaries(
            @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(
                documentSummaryService.getSummaries(documentId)
        );
    }

    @GetMapping("/{summaryId}")
    @RequireFeature(code = "DOC_SUMMARY")
    public ResponseEntity<SummaryContentDto> getSummary(
            @PathVariable Long summaryId
    ) {
        return ResponseEntity.ok(
                documentSummaryService.getSummary(summaryId)
        );
    }
}
