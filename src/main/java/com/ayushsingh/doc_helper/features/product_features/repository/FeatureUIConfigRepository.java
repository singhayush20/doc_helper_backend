package com.ayushsingh.doc_helper.features.product_features.repository;

import com.ayushsingh.doc_helper.features.product_features.entity.FeatureUIConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeatureUIConfigRepository extends JpaRepository<FeatureUIConfig, Long> {
    Boolean existsByFeatureIdAndFeatureUiVersion(Long productFeatureId,
                                           Integer featureUIVersion);

    List<FeatureUIConfig> findByFeatureIdInAndScreenAndActiveTrue(
            List<Long> featureIds,
            String screen
    );
}
