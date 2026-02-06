package com.ayushsingh.doc_helper.features.doc_summary.controller;

import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryContentDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateRequestDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateResponseDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryMetadataDto;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentSummaryController {

    private final DocumentSummaryService documentSummaryService;

    @PostMapping("/documents/{documentId}/summaries")
    public ResponseEntity<SummaryCreateResponseDto> createSummary(
            @PathVariable Long documentId,
            @RequestBody SummaryCreateRequestDto request
    ) {
        return ResponseEntity.ok(
                documentSummaryService.createSummary(documentId, request)
        );
    }

    @GetMapping("/documents/{documentId}/summaries")
    public ResponseEntity<List<SummaryMetadataDto>> getSummaries(
            @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(
                documentSummaryService.getSummaries(documentId)
        );
    }

    @GetMapping("/summaries/{summaryId}")
    public ResponseEntity<SummaryContentDto> getSummary(
            @PathVariable Long summaryId
    ) {
        return ResponseEntity.ok(
                documentSummaryService.getSummary(summaryId)
        );
    }
}
