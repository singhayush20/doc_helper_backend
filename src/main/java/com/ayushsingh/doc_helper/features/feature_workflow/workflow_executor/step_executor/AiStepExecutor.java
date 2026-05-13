package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.step_executor;

import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.StepExecutionResult;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowExecutionContext;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflowStep;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowStepExecutionRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.StepExecutor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiStepExecutor implements StepExecutor {

    // private final AiService aiService;
    private final WorkflowStepExecutionRepository stepExecutionRepository;

    @Override
    public StepExecutionResult execute(
            FeatureWorkflowStep step,
            WorkflowExecution execution,
            WorkflowExecutionContext context) {

        var stepExecution = (WorkflowStepExecution) context.get("currentStepExecution");

        // Call AI
        // String result = aiService.generate(context);
        String result = "";

        // Store output
        stepExecution.setOutputJson(result);
        stepExecutionRepository.save(stepExecution);

        return StepExecutionResult.builder()
                .stepId(step.getWorkflowStepId())
                .stepName(step.getName())
                .instruction(step.getInstruction())
                .status("COMPLETED")
                .isTerminal(false)
                .build();
    }
}