package com.ayushsingh.doc_helper.features.doc_summary.dto;

import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryTone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryResponseDto {
    private Long summaryId;
    private Integer version;
    private SummaryTone tone;
    private SummaryLength length;
    private Integer tokensUsed;
    private Integer wordCount;
    private String content;
    private Instant createdAt;
}
