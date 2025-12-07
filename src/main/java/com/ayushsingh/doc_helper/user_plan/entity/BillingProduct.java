package com.ayushsingh.doc_helper.user_plan.entity;

import java.util.Set;

import org.springframework.data.annotation.Id;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // TODO: Code and Account Tier seems to be duplicating each other

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 50)
    private AccountTier tier;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "monthly_token_limit", nullable = false)
    private Long monthlyTokenLimit;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, orphanRemoval = false, cascade = { CascadeType.PERSIST,
            CascadeType.MERGE, CascadeType.REFRESH })
    private Set<BillingPrice> billingPrices;
}