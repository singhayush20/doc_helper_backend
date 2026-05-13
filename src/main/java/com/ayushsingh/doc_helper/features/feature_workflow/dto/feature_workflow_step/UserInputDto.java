package com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step;

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
public class UserInputDto {
    private String message;
    private String uploadedFileName;
}
