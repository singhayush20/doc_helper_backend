package com.ayushsingh.doc_helper.features.doc_summary.executor;

import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryCreateResponseDto;
import com.ayushsingh.doc_helper.features.doc_summary.executor_command.DocSummaryCommand;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentSummaryService;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureExecutor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocSummaryExecutor implements FeatureExecutor<DocSummaryCommand, SummaryCreateResponseDto> {

    private final DocumentSummaryService documentSummaryService;

    @Override
    public FeatureCodes featureCode() {
        return FeatureCodes.DOC_SUMMARY;
    }

    @Override
    public SummaryCreateResponseDto execute(DocSummaryCommand input) {
        return documentSummaryService.createSummary(
                input.documentId(),
                input.request()
        );
    }
}
