package com.ayushsingh.doc_helper.features.doc_summary.dto;

import java.time.Instant;

import com.google.auto.value.AutoValue.Builder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDetailsDto {
    
    private Long documentId;
    private String fileName;
    private String originalFilename;
    private Instant createdAt;
}
