package com.ayushsingh.doc_helper.user_plan.entity;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "billing_price", indexes = {
        @Index(name = "idx_price_product", columnList = "product_id"),
        @Index(name = "idx_price_code_version", columnList = "price_code, version", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "PRO_MONTHLY_V1"
    @Column(name = "price_code", nullable = false, length = 100)
    private String priceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private BillingProduct product;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false, length = 20)
    private BillingPeriod billingPeriod;

    // Version for same logical plan (grandfathering)
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    // Razorpay plan/price id
    @Column(name = "provider_plan_id", nullable = false, length = 100)
    private String providerPlanId;

    @Column(name = "active", nullable = false)
    private boolean active;
}
