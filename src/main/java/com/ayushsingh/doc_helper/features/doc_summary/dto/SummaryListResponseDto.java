package com.ayushsingh.doc_helper.features.doc_summary.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryListResponseDto {
    private List<SummaryMetadataDto> summaries;
}
