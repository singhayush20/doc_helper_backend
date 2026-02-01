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
@Table(name = "feature_ui_config")
@Getter
@Setter
public class FeatureUIConfig {

    @Id
    @Column(name="feature_id")
    private Long featureId;

    private String icon;

    private String backgroundColor;
    private String textColor;
    private String badgeText;

    private Integer sortOrder;
    private boolean visible;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FeatureUIConfig that = (FeatureUIConfig) o;
        return Objects.equals(featureId, that.featureId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(featureId);
    }

    @Override
    public String toString() {
        return "FeatureUIConfig{" +
                "featureId=" + featureId +
                ", icon='" + icon + '\'' +
                ", backgroundColor='" + backgroundColor + '\'' +
                ", textColor='" + textColor + '\'' +
                ", badgeText='" + badgeText + '\'' +
                ", sortOrder=" + sortOrder +
                ", visible=" + visible +
                '}';
    }
}

