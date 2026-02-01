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
        name = "usage_quota",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"userId", "featureCode", "metric"}
        )
)
@Getter
@Setter
public class UsageQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id",nullable = false)
    private Long userId;

    @Column(name="feature_code",nullable = false)
    private String featureCode;

    private String metric;

    private Long used;

    private Long limit;

    @Column(name="reset_at",nullable = false)
    private Instant resetAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UsageQuota that = (UsageQuota) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "UsageQuota{" +
                "id=" + id +
                ", userId=" + userId +
                ", featureCode='" + featureCode + '\'' +
                ", metric='" + metric + '\'' +
                ", used=" + used +
                ", limit=" + limit +
                ", resetAt=" + resetAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

