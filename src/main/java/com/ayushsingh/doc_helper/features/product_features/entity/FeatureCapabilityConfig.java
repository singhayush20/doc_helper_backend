package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "feature_capability_config")
@Getter
@Setter
public class FeatureCapabilityConfig {

    @Id
    private Long featureId;

    @Column(columnDefinition = "jsonb")
    private String config;
}

