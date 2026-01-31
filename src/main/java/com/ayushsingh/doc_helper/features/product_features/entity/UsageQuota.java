package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

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

    private Long userId;
    private String featureCode;
    private String metric;

    private Long used;
    private Long limit;

    private Instant resetAt;
}

