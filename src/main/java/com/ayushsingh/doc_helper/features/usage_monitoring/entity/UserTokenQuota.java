package com.ayushsingh.doc_helper.features.usage_monitoring.entity;

import java.time.Instant;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ayushsingh.doc_helper.features.user.entity.User;

@Entity
@Table(name = "user_token_quota", indexes = {
        @Index(name = "idx_quota_user_id", columnList = "user_id", unique = true),
        @Index(name = "idx_quota_reset_date", columnList = "reset_date")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserTokenQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // userId is mapped directly to same FK column
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "monthly_limit", nullable = false)
    private Long monthlyLimit;

    @Column(name = "current_monthly_usage", nullable = false)
    private Long currentMonthlyUsage;

    @Column(name = "reset_date", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant resetDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;
}
