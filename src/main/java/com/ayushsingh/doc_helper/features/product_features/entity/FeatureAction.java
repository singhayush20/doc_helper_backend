package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "feature_actions")
@Getter
@Setter
public class FeatureAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionKind kind;

    private String destination;

    @Column(columnDefinition = "jsonb")
    private String payload;

    private boolean enabled;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FeatureAction that = (FeatureAction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "FeatureAction{" +
                "id=" + id +
                ", featureId=" + featureId +
                ", kind=" + kind +
                ", destination='" + destination + '\'' +
                ", payload='" + payload + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}

