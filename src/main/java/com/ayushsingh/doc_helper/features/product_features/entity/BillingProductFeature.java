package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    private Long billingProductId;
    private Long featureId;

    private boolean enabled;
    private Integer priority;
}

