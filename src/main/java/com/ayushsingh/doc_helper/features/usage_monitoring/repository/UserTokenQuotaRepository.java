package com.ayushsingh.doc_helper.features.usage_monitoring.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;

public interface UserTokenQuotaRepository extends JpaRepository<UserTokenQuota, Long> {

        Optional<UserTokenQuota> findByUserId(Long userId);

        /**
         * Find quotas that need to be reset with pagination (better for large datasets)
         */
        @Query("SELECT q FROM UserTokenQuota q WHERE q.resetDate <= :now AND q.isActive = true")
        Page<UserTokenQuota> findQuotasToResetPaginated(@Param("now") Instant now, Pageable pageable);

        @Modifying
        @Query("UPDATE UserTokenQuota q SET q.currentMonthlyUsage = q.currentMonthlyUsage + :tokens " +
                        "WHERE q.userId = :userId")
        void incrementUsage(
                        @Param("userId") Long userId,
                        @Param("tokens") Long tokens
                );

        /**
         * Reset quota with atomic update
         */
        @Modifying
        @Query("UPDATE UserTokenQuota q SET q.currentMonthlyUsage = 0, " +
               "q.resetDate = :newResetDate WHERE q.userId = :userId")
        void resetQuota(
                @Param("userId") Long userId,
                @Param("newResetDate") Instant newResetDate);
}
