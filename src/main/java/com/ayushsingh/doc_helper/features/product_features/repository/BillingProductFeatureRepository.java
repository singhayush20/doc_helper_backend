package com.ayushsingh.doc_helper.features.product_features.repository;

import com.ayushsingh.doc_helper.features.product_features.entity.BillingProductFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}
