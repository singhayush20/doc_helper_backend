package com.ayushsingh.doc_helper.features.product_features.repository;

import com.ayushsingh.doc_helper.features.product_features.entity.UsageMetric;
import com.ayushsingh.doc_helper.features.product_features.entity.UsageQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;

public interface UsageQuotaRepository
        extends JpaRepository<UsageQuota, Long> {

    Optional<UsageQuota> findByUserIdAndFeatureCodeAndMetric(
            Long userId, String featureCode, UsageMetric metric
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

    @Query("""
        select q
        from UsageQuota q
        where q.resetAt is not null
          and q.resetAt <= :now
    """)
    Page<UsageQuota> findQuotasToResetPaginated(
            @Param("now") Instant now,
            Pageable pageable
    );

    @Modifying
    @Query("""
        update UsageQuota q
        set q.used = q.used + :amount
        where q.userId = :userId
          and q.featureCode = :featureCode
          and q.metric = :metric
          and q.used + :amount <= q.limit
    """)
    int consumeIfAvailable(
            @Param("userId") Long userId,
            @Param("featureCode") String featureCode,
            @Param("metric") UsageMetric metric,
            @Param("amount") long amount
    );
}

