package com.ayushsingh.doc_helper.features.payments.entity;

import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user_plan.entity.Subscription;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "payment_transaction", indexes = {
        @Index(name = "idx_tx_user", columnList = "user_id"),
        @Index(name = "idx_tx_subscription", columnList = "subscription_id"),
        @Index(name = "idx_tx_provider_payment", columnList = "provider_payment_id")

})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "provider_payment_id", length = 100, unique = true)
    private String providerPaymentId;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PaymentType type;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Type(JsonType.class)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private String rawPayload;

}
