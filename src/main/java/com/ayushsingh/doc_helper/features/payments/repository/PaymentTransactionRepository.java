package com.ayushsingh.doc_helper.features.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.payments.entity.PaymentTransaction;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    boolean existsByProviderPaymentId(String providerPaymentId);

    Optional<PaymentTransaction> findByProviderPaymentId(String providerPaymentId);
}
