package com.ayushsingh.doc_helper.features.feature_workflow.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Table(name = "workflow_action_execution", indexes = {
        @Index(name = "idx_action_execution_step", columnList = "workflow_step_execution_id"),
        @Index(name = "idx_action_execution_selected", columnList = "workflow_step_execution_id, selected")
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowActionExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_action_execution_id", nullable = false, unique = true)
    private Integer workflowActionExecutionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_step_execution_id", nullable = false)
    private WorkflowStepExecution stepExecution;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "value", nullable = false)
    private String value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "selected", nullable = false)
    @Builder.Default
    private Boolean selected = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((workflowActionExecutionId == null) ? 0 : workflowActionExecutionId.hashCode());
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
        WorkflowActionExecution other = (WorkflowActionExecution) obj;
        if (workflowActionExecutionId == null) {
            if (other.workflowActionExecutionId != null)
                return false;
        } else if (!workflowActionExecutionId.equals(other.workflowActionExecutionId))
            return false;
        return true;
    }

    
}