package com.ayushsingh.doc_helper.features.feature_workflow.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.StepActionType;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.StepActionUiType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "workflow_step_action_config", indexes = {
        @Index(name = "idx_action_step_id", columnList = "workflow_step_id"),
        @Index(name = "idx_action_type", columnList = "action_type")
})
@Entity
public class WorkflowStepActionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_ui_id", nullable = false, unique = true)
    private Integer stepUiId;

    @Column(name = "action_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StepActionType actionType;

    @Column(name = "action_ui_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StepActionUiType actionUiType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_id", nullable = false)
    private FeatureWorkflowStep workflowStep;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ui_config", columnDefinition = "jsonb", nullable = false)
    private String uiJson;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((stepUiId == null) ? 0 : stepUiId.hashCode());
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
        WorkflowStepActionConfig other = (WorkflowStepActionConfig) obj;
        if (stepUiId == null) {
            if (other.stepUiId != null)
                return false;
        } else if (!stepUiId.equals(other.stepUiId))
            return false;
        return true;
    }

}
