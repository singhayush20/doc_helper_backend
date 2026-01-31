package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_events")
@Getter
@Setter
public class UsageEvent {

    @Id
    private UUID id;

    private Long userId;
    private String featureCode;
    private String metric;
    private Long amount;

    private Instant createdAt;
}

