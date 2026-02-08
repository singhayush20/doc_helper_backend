package com.ayushsingh.doc_helper.features.user_plan.entity;

import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.ArrayList;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_product")
public class BillingProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50,updatable = false)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tier", nullable = false, length = 50)
    private AccountTier tier;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "monthly_token_limit", nullable = false)
    private Long monthlyTokenLimit;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "billing_product_features", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "feature_index")
    @Column(name = "feature", length = 512)
    @Builder.Default
    private List<String> features = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, orphanRemoval = false, cascade = { CascadeType.PERSIST,
            CascadeType.MERGE, CascadeType.REFRESH })
    private Set<BillingPrice> billingPrices;
}