package com.ayushsingh.doc_helper.features.user_plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;

import java.util.List;
import java.util.Optional;

public interface BillingPriceRepository extends JpaRepository<BillingPrice, Long> {

    // For checkout: always fetch the latest active version by priceCode
    Optional<BillingPrice> findFirstByPriceCodeAndActiveTrueOrderByVersionDesc(String priceCode);

    // For Razorpay subscriptions (PlanID stored in Razorpay dashboard)
    Optional<BillingPrice> findByProviderPlanId(String providerPlanId);

    // List prices for UI (e.g. PRO has multiple plans: monthly, yearly)
    List<BillingPrice> findByProductIdAndActiveTrue(Long productId);

    // Optional advanced filtering
    List<BillingPrice> findByProductId(Long productId);
}
