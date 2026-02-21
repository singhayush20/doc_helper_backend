package com.ayushsingh.doc_helper.features.doc_summary.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryCreateRequestDto {
    private Long documentId;
    private String tone;   // CASUAL, PROFESSIONAL
    private String length; // SHORT, MEDIUM, LONG
}
