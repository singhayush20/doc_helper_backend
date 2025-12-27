package com.ayushsingh.doc_helper.features.user_plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;

import java.util.List;
import java.util.Optional;

public interface BillingPriceRepository extends JpaRepository<BillingPrice, Long> {

    // For checkout: always fetch the latest active version by priceCode
    Optional<BillingPrice> findFirstByPriceCodeAndActiveTrueOrderByVersionDesc(String priceCode);

    // For Razorpay subscriptions (PlanID stored in Razorpay dashboard)
    Optional<BillingPrice> findByProviderPlanId(String providerPlanId);

    // Optional advanced filtering
    List<BillingPrice> findByProductId(Long productId);

    @Query("""
                SELECT COUNT(p) > 0
                FROM BillingPrice p
                WHERE p.product.id = :productId
            """)
    boolean existsByProductId(@Param("productId") Long productId);

    List<BillingPrice> findByProductIdAndActiveTrue(Long productId);
}
