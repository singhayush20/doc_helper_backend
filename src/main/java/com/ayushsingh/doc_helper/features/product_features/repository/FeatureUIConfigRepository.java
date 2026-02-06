package com.ayushsingh.doc_helper.features.product_features.repository;

import com.ayushsingh.doc_helper.features.product_features.entity.FeatureUIConfig;
import com.ayushsingh.doc_helper.features.product_features.entity.UIComponentType;
import com.ayushsingh.doc_helper.features.product_features.repository.projections.FeatureUiConfigView;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeatureUIConfigRepository extends JpaRepository<FeatureUIConfig, Long> {
    Boolean existsByFeatureIdAndFeatureUiVersion(Long productFeatureId,
                                                 Integer featureUIVersion);

    List<FeatureUIConfig> findByFeatureIdInAndScreenAndActiveTrue(
            List<Long> featureIds,
            String screen
    );

    @Query("""
                select f as feature, u as uiConfig, bpf.priority as priority
                from BillingProductFeature bpf
                join Feature f on f.id = bpf.featureId
                join FeatureUIConfig u on u.featureId = f.id
                where bpf.billingProductId = :billingProductId
                  and u.featureUiVersion = bpf.enabledVersion
                  and bpf.enabled = true
                  and f.active = true
                  and u.active = true
                  and u.screen = :screen
                  and u.componentType = :componentType
            """)
    // 1. Feature is enabled for a billing product
    // 2. Feature itself is enabled globally
    // 3. UIConfig is enabled
    // 4. UIConfig is for the correct screen
    // 5. UIConfig is for the correct component type
    // 6. Enabled ui version matches the ui config version
    List<FeatureUiConfigView> findEnabledUiConfigsForProduct(
            @Param("billingProductId") Long billingProductId,
            @Param("screen") String screen,
            @Param("componentType") UIComponentType componentType
    );
}
