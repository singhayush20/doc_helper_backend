package com.ayushsingh.doc_helper.features.product_features.repository;

import com.ayushsingh.doc_helper.features.product_features.entity.BillingProductFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillingProductFeatureRepository
        extends JpaRepository<BillingProductFeature, Long> {

    List<BillingProductFeature> findByBillingProductIdAndEnabledTrue(Long billingProductId);

    @Query("""
        select pf
        from BillingProductFeature pf
        where pf.billingProductId = :billingProductId
          and pf.enabled = true
    """)
    List<BillingProductFeature> findEnabledByBillingProduct(
            @Param("billingProductId") Long billingProductId
    );

    Optional<BillingProductFeature> findByBillingProductIdAndFeatureId(
            Long billingProductId,
            Long featureId
    );

    Optional<BillingProductFeature> findByBillingProductIdAndFeatureIdAndEnabledTrue(
            Long billingProductId,
            Long featureId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update BillingProductFeature pf
        set pf.enabled = :enabled
        where pf.billingProductId = :billingProductId
          and pf.featureId = :featureId
    """)
    int updateEnabledForProductFeature(
            @Param("billingProductId") Long billingProductId,
            @Param("featureId") Long featureId,
            @Param("enabled") boolean enabled
    );
}
