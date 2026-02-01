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
                columnNames = {"billingProductId", "featureId"}
        )
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

    private boolean enabled;

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

