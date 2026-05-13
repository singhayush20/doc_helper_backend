package com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step;

import java.util.HashSet;
import java.util.Set;

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
public class GeneratedContentDto {
    
    private String platform; // social media platform
    private String textInfo; // markdown text
    private String image; // base64 image

    @Builder.Default
    private Set<String> tags = new HashSet<>();
}
