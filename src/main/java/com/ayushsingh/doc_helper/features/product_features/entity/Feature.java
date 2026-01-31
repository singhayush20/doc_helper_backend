package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "features")
@Getter
@Setter
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String code;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private FeatureType type;

    private boolean active;
}
