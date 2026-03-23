package com.ayushsingh.doc_helper.features.feature_workflow.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.WorkflowExecutionStatus;

import jakarta.persistence.Index;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name="workflow_execution",indexes = {
        @Index(name = "idx_execution_user_status", columnList = "user_id, status"),
        @Index(name = "idx_execution_workflow", columnList = "workflow_id")
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="workflow_execution_id",nullable=false,unique=true)
    private Integer workflowExecutionId;

    @Column(name="user_id",nullable=false)
    private Long userId;

    @Column(name="workflow_id",nullable = false)
    private Integer workflowId;

    @Column(name="current_step_id",nullable = false)
    private Integer currentStepId;

    @Column(name="status",nullable=false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private WorkflowExecutionStatus status;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((workflowExecutionId == null) ? 0 : workflowExecutionId.hashCode());
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
        WorkflowExecution other = (WorkflowExecution) obj;
        if (workflowExecutionId == null) {
            if (other.workflowExecutionId != null)
                return false;
        } else if (!workflowExecutionId.equals(other.workflowExecutionId))
            return false;
        return true;
    }

    
}
