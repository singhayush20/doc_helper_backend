package com.ayushsingh.doc_helper.features.product_features.repository;

import com.ayushsingh.doc_helper.features.product_features.entity.FeatureAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureActionRepository
        extends JpaRepository<FeatureAction, Long> {

    Optional<FeatureAction> findByFeatureIdAndEnabledTrue(Long featureId);

    void deleteByFeatureId(Long id);
}

