package com.ayushsingh.doc_helper.user_plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.user_plan.entity.BillingPrice;

import java.util.Optional;

public interface BillingPriceRepository extends JpaRepository<BillingPrice, Long> {

    Optional<BillingPrice> findFirstByPriceCodeAndActiveTrueOrderByVersionDesc(String priceCode);

    Optional<BillingPrice> findByProviderPlanId(String providerPlanId);
}
