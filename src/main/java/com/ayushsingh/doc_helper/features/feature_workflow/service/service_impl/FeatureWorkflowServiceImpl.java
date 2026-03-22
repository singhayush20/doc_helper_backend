package com.ayushsingh.doc_helper.features.feature_workflow.service.service_impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.WorkflowStepActionConfigDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowCreateDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto_to_entity_mapper.WorkflowEntityMapper;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflow;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflowStep;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActionConfig;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.FeatureWorkflowRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowStepActionConfigRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.service.FeatureWorkflowService;
import com.ayushsingh.doc_helper.features.product_features.service.AdminFeatureService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureWorkflowServiceImpl implements FeatureWorkflowService {

    private final FeatureWorkflowRepository workflowRepository;
    private final WorkflowStepActionConfigRepository actionConfigRepository;
    private final WorkflowEntityMapper workflowEntityMapper;
    private final AdminFeatureService adminFeatureService;

    @Override
    @Transactional
    public WorkflowDetailsDto createWorkflow(@Valid WorkflowCreateDto workflowCreateDto) {
        if (workflowRepository.existsByName(workflowCreateDto.getName())) {
            throw new BaseException("Workflow already exists", ExceptionCodes.DUPLICATE_FEATURE_ERROR);
        }

        FeatureWorkflow workflow = workflowEntityMapper.toWorkflowEntity(workflowCreateDto);
        var createStepDtos = workflowCreateDto.getSteps();

        if (createStepDtos != null && !createStepDtos.isEmpty()) {
            var steps = createStepDtos.stream()
                    .map(stepDto -> {
                        var workflowStep = workflowEntityMapper.toStepEntity(stepDto, workflow);
                        var actions = stepDto.getActions() == null
                                ? List.<WorkflowStepActionConfig>of()
                                : stepDto.getActions().stream()
                                        .map(actionDto -> workflowEntityMapper.toActionEntity(actionDto, workflowStep))
                                        .toList();
                        workflowStep.setActions(new ArrayList<>(actions));
                        return workflowStep;
                    })
                    .toList();
            workflow.setSteps(new ArrayList<>(steps));
        }

        FeatureWorkflow savedWorkflow = workflowRepository.save(workflow);
        adminFeatureService.assignWorkflowToFeature(workflowCreateDto.getFeatureId(), savedWorkflow);

        return workflowEntityMapper.toWorkflowDetailsDto(savedWorkflow);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowDetailsDto getWorkflowById(Integer workflowId) {
        var workflow = workflowRepository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new BaseException("Workflow not found", ExceptionCodes.WORKFLOW_NOT_FOUND));
        return mapWorkflowDetails(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowDetailsDto> getAllWorkflows() {
        return workflowRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::mapWorkflowDetails));
    }

    @Override
    @Transactional
    public void deleteWorkflow(Integer workflowId) {
        var workflow = workflowRepository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new BaseException("Workflow not found", ExceptionCodes.WORKFLOW_NOT_FOUND));

        var feature = workflow.getFeature();
        if (feature != null) {
            adminFeatureService.unassignWorkflowFromFeature(feature.getId());
        }

        workflowRepository.delete(workflow);
    }

    private List<WorkflowDetailsDto> mapWorkflowDetails(List<FeatureWorkflow> workflows) {
        if (workflows.isEmpty()) {
            return List.of();
        }

        var actionsByStepId = loadActionsByStepId(workflows);
        return workflows.stream()
                .map(workflow -> mapWorkflowDetails(workflow, actionsByStepId))
                .toList();
    }

    private WorkflowDetailsDto mapWorkflowDetails(FeatureWorkflow workflow) {
        var actionsByStepId = loadActionsByStepId(List.of(workflow));
        return mapWorkflowDetails(workflow, actionsByStepId);
    }

    private WorkflowDetailsDto mapWorkflowDetails(
            FeatureWorkflow workflow,
            Map<Integer, List<WorkflowStepActionConfigDetailsDto>> actionsByStepId) {
        return WorkflowDetailsDto.builder()
                .workflowId(workflow.getWorkflowId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .steps(mapStepDetails(workflow.getSteps(), actionsByStepId))
                .build();
    }

    private List<FeatureWorkflowStepDetailsDto> mapStepDetails(
            List<FeatureWorkflowStep> steps,
            Map<Integer, List<WorkflowStepActionConfigDetailsDto>> actionsByStepId) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }

        return steps.stream()
                .map(step -> FeatureWorkflowStepDetailsDto.builder()
                        .workflowStepId(step.getWorkflowStepId())
                        .name(step.getName())
                        .stepOrder(step.getStepOrder())
                        .stepActor(step.getStepActor())
                        .instruction(step.getInstruction())
                        .actions(actionsByStepId.getOrDefault(step.getWorkflowStepId(), List.of()))
                        .build())
                .toList();
    }

    private Map<Integer, List<WorkflowStepActionConfigDetailsDto>> loadActionsByStepId(List<FeatureWorkflow> workflows) {
        var stepIds = workflows.stream()
                .map(FeatureWorkflow::getSteps)
                .flatMap(List::stream)
                .map(FeatureWorkflowStep::getWorkflowStepId)
                .toList();

        if (stepIds.isEmpty()) {
            return Map.of();
        }

        return actionConfigRepository.findAllActionConfigsByWorkflowStepIds(stepIds)
                .stream()
                .collect(Collectors.groupingBy(
                        action -> action.getWorkflowStep().getWorkflowStepId(),
                        LinkedHashMap::new,
                        Collectors.mapping(workflowEntityMapper::toActionDetailsDto, Collectors.toList())));
    }
}
