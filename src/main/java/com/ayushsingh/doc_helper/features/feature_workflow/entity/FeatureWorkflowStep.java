package com.ayushsingh.doc_helper.features.feature_workflow.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.converters.WorkflowStepConfigConverter;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.WorkflowStepActor;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Entity
@Table(name = "feature_workflow_step", indexes = {
        @Index(name = "idx_step_workflow_id", columnList = "workflow_id"),
        @Index(name = "idx_step_order", columnList = "workflow_id, step_order"),
        @Index(name = "idx_step_actor", columnList = "step_actor")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeatureWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_step_id", nullable = false, unique = true)
    private Integer workflowStepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", referencedColumnName = "workflow_id", nullable = false)
    private FeatureWorkflow workflow;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_actor", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private WorkflowStepActor stepActor;

    // instruction - set by admin or added from AI call
    @Column(name = "instruction")
    private String instruction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = WorkflowStepConfigConverter.class)
    @Column(name = "step_config", columnDefinition = "jsonb")
    private WorkflowStepConfig stepConfig;

    @OneToMany(mappedBy = "workflowStep", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkflowStepActionConfig> actions = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((workflowStepId == null) ? 0 : workflowStepId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeatureWorkflowStep other = (FeatureWorkflowStep) obj;
        if (workflowStepId == null) {
            if (other.workflowStepId != null)
                return false;
        } else if (!workflowStepId.equals(other.workflowStepId))
            return false;
        return true;
    }

}
