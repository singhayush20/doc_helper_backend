package com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step;

import java.util.ArrayList;
import java.util.List;

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
public class StepResultDto {

    private String systemMessage; // system or ai-generated message

    @Builder.Default
    private List<GeneratedContentDto> content = new ArrayList<>();

}
