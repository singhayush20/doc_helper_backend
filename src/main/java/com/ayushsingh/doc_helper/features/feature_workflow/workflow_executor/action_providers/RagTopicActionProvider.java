package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.action_providers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.ActionDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowExecutionContext;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.StepActionUiType;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.ActionProvider;

import lombok.RequiredArgsConstructor;

@Component("RAG_TOPIC_EXTRACTOR")
@RequiredArgsConstructor
public class RagTopicActionProvider implements ActionProvider {

    // private final RagService ragService;

    @Override
    public List<ActionDto> resolve(WorkflowExecutionContext context) {

        String documentId = context.get("documentId");

        // List<String> topics = ragService.extractTopics(documentId);
        List<String> topics = new ArrayList<>();

        return topics.stream()
                .map((String topic) -> ActionDto.builder()
                        .label(topic)
                        .value(topic.toLowerCase().replace(" ", "_"))
                        .uiType(StepActionUiType.CHIP)
                        .selected(false)
                        .build())
                .toList();
    }
}
