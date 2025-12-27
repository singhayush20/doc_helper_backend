package com.ayushsingh.doc_helper.features.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.payments.entity.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    boolean existsByProviderPaymentId(String providerPaymentId);
}
