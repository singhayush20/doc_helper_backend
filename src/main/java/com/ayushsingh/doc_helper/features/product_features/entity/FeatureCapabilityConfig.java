package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "feature_capability_config")
@Getter
@Setter
public class FeatureCapabilityConfig {

    @Id
    private Long featureId;

    @Column(columnDefinition = "jsonb")
    private String config;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FeatureCapabilityConfig that = (FeatureCapabilityConfig) o;
        return Objects.equals(featureId, that.featureId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(featureId);
    }

    @Override
    public String toString() {
        return "FeatureCapabilityConfig{" +
                "featureId=" + featureId +
                ", config='" + config + '\'' +
                '}';
    }
}

