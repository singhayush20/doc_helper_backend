package com.ayushsingh.doc_helper.features.usage_monitoring.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_token_quota", indexes = {
        @Index(name = "idx_quota_user_id", columnList = "user_id", unique = true),
        @Index(name = "idx_quota_reset_date", columnList = "reset_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "monthly_limit", nullable = false)
    @Builder.Default
    private Long monthlyLimit = 100000L;

    @Column(name = "current_monthly_usage", nullable = false)
    @Builder.Default
    private Long currentMonthlyUsage = 0L;

    @Column(name = "reset_date", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant resetDate;

    @Column(name = "tier", length = 50)
    @Builder.Default
    private String tier = "free";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
