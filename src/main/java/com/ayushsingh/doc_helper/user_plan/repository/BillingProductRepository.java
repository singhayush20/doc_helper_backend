package com.ayushsingh.doc_helper.user_plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.user_plan.entity.BillingProduct;

import java.util.Optional;

public interface BillingProductRepository extends JpaRepository<BillingProduct, Long> {
    Optional<BillingProduct> findByCodeAndActiveTrue(String code);
}
