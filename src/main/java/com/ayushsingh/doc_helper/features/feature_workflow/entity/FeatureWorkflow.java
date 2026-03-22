package com.ayushsingh.doc_helper.features.feature_workflow.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ayushsingh.doc_helper.features.product_features.entity.Feature;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Entity
@Table(name = "feature_workflow", indexes = {
        @Index(name = "idx_workflow_name", columnList = "workflow_name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeatureWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_id", nullable = false, unique = true)
    private Integer workflowId;

    @Column(name = "workflow_name", unique = true, nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @OneToOne(mappedBy = "workflow", fetch = FetchType.LAZY)
    private Feature feature;

    @OneToMany(mappedBy = "workflow", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @Builder.Default
    private List<FeatureWorkflowStep> steps = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + workflowId;
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
        FeatureWorkflow other = (FeatureWorkflow) obj;
        if (workflowId != other.workflowId)
            return false;
        return true;
    }
}
