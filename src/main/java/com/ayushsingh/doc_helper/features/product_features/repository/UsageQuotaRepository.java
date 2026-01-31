package com.ayushsingh.doc_helper.features.product_features.repository;

import com.ayushsingh.doc_helper.features.product_features.entity.UsageQuota;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsageQuotaRepository
        extends JpaRepository<UsageQuota, Long> {

    Optional<UsageQuota> findByUserIdAndFeatureCodeAndMetric(
            Long userId, String featureCode, String metric
    );

    @Query("""
        select q
        from UsageQuota q
        where q.userId = :userId
          and q.featureCode in :featureCodes
    """)
    List<UsageQuota> findByUserAndFeatureCodes(
            @Param("userId") Long userId,
            @Param("featureCodes") Collection<String> featureCodes
    );
}

