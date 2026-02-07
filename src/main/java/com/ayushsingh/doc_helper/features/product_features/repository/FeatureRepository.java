package com.ayushsingh.doc_helper.features.product_features.repository;

import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    Optional<Feature> findByCodeAndActiveTrue(FeatureCodes code);

    @Query("""
        select f
        from Feature f
        where f.id in :ids
          and f.active = true
    """)
    List<Feature> findActiveByIds(@Param("ids") Collection<Long> ids);

    Optional<Feature> findByCode(FeatureCodes code);
}
