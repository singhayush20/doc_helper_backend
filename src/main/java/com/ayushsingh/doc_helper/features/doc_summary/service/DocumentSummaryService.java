package com.ayushsingh.doc_helper.features.doc_summary.service;

import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryContentDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateRequestDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryListResponseDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryResponseDto;

public interface DocumentSummaryService {
    SummaryResponseDto createSummary(SummaryCreateRequestDto request);

    SummaryListResponseDto getSummaries(Long documentId);

    SummaryContentDto getSummary(Long summaryId);

    void deleteSummaryByDocumentId(Long documentId);
}
