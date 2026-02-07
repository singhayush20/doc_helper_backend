package com.ayushsingh.doc_helper.features.doc_summary.service;

import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryContentDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateRequestDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateResponseDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryMetadataDto;

import java.util.List;

public interface DocumentSummaryService {
    SummaryCreateResponseDto createSummary(Long documentId, SummaryCreateRequestDto request);

    List<SummaryMetadataDto> getSummaries(Long documentId);

    SummaryContentDto getSummary(Long summaryId);
}
