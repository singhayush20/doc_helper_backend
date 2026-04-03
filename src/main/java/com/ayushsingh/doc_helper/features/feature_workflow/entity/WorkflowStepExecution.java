package com.ayushsingh.doc_helper.features.feature_workflow.entity;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.StepExecutionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Table(name = "workflow_step_execution", indexes = {
        @Index(name = "idx_step_execution_execution", columnList = "workflow_execution_id"),
        @Index(name = "idx_step_execution_order", columnList = "workflow_execution_id, step_order"),
        @Index(name = "idx_unique_step_per_execution",
                columnList = "workflow_execution_id, step_id, version",
                unique = true)
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_step_execution_id", nullable = false, unique = true)
    private Integer workflowStepExecutionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_execution_id", nullable = false)
    private WorkflowExecution workflowExecution;

    @Column(name = "step_id", nullable = false)
    private Integer stepId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    /**
     * USER input / system input
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_json", columnDefinition = "jsonb")
    private String inputJson;

    /**
     * AI/System output
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_json", columnDefinition = "jsonb")
    private String outputJson;

    /**
     * Needed for async + retries
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private StepExecutionStatus status;

    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
