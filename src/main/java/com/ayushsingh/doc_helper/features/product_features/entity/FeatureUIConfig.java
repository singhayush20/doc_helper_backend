package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "feature_ui_config")
@Getter
@Setter
public class FeatureUIConfig {

    @Id
    private Long featureId;

    private String icon;
    private String backgroundColor;
    private String textColor;
    private String badgeText;

    private Integer sortOrder;
    private boolean visible;
    private boolean showInPremiumGrid;
}

