package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "feature_actions")
@Getter
@Setter
public class FeatureAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long featureId;

    @Enumerated(EnumType.STRING)
    private ActionKind kind;

    private String destination;

    @Column(columnDefinition = "jsonb")
    private String payload;

    private boolean enabled;
}

