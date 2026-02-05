package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "billing_product_features",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"billing_product_id", "feature_id"}
        ),
        indexes = {
                @Index(
                        name = "idx_bpf_product_enabled",
                        columnList = "billing_product_id, enabled"
                ),
                @Index(
                        name = "idx_bpf_product_priority",
                        columnList = "billing_product_id, priority"
                )
        }
)
@Getter
@Setter
public class BillingProductFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="billing_product_id",nullable = false)
    private Long billingProductId;

    @Column(name="feature_id",nullable = false)
    private Long featureId;

    // The version of the feature that is enabled for this billing product <>
    // feature. Match this version with the FeatureUIConfig featureUIVersion,
    // to get the ui config for a particular version
    @Column(name = "enabled_version",nullable=false)
    private Long enabledVersion;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Integer priority;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BillingProductFeature that = (BillingProductFeature) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "BillingProductFeature{" +
                "id=" + id +
                ", billingProductId=" + billingProductId +
                ", featureId=" + featureId +
                ", enabled=" + enabled +
                ", priority=" + priority +
                '}';
    }
}

