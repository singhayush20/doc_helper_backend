package com.ayushsingh.doc_helper.features.doc_summary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryCreateResponseDto {
    private Long summaryId;
    private Integer version;
    private Integer tokensUsed;
    private String content;
    private Integer wordCount;
}
