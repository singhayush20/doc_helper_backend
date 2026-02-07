package com.ayushsingh.doc_helper.features.doc_summary.executor_command;

import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateRequestDto;

public record DocSummaryCommand(Long documentId, SummaryCreateRequestDto request) {
}
