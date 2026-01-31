package com.ayushsingh.doc_helper.features.product_features.repository;

import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    Optional<Feature> findByCodeAndActiveTrue(String code);

    @Query("""
        select f
        from Feature f
        where f.id in :ids
          and f.active = true
    """)
    List<Feature> findActiveByIds(@Param("ids") Collection<Long> ids);
}
