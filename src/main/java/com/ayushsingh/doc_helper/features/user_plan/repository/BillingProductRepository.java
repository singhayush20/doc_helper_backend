package com.ayushsingh.doc_helper.features.user_plan.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingProduct;

import java.util.List;
import java.util.Optional;

public interface BillingProductRepository extends JpaRepository<BillingProduct, Long> {

    Optional<BillingProduct> findByTier(AccountTier code);

    @EntityGraph(attributePaths = "features")
    List<BillingProduct> findByActiveTrue();
}
