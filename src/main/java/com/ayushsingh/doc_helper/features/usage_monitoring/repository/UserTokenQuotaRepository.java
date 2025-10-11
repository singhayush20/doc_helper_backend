package com.ayushsingh.doc_helper.features.usage_monitoring.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenQuota;

public interface UserTokenQuotaRepository extends JpaRepository<UserTokenQuota, Long> {

        Optional<UserTokenQuota> findByUserId(Long userId);

        boolean existsByUserId(Long userId);

        // Find all quotas that need to be reset
        @Query("SELECT q FROM UserTokenQuota q WHERE q.resetDate <= :now")
        List<UserTokenQuota> findQuotasToReset(@Param("now") Instant now);

        // Update quota usage atomically
        @Modifying
        @Query("UPDATE UserTokenQuota q SET q.currentMonthlyUsage = q.currentMonthlyUsage + :tokens " +
                        "WHERE q.userId = :userId")
        int incrementUsage(
                        @Param("userId") Long userId,
                        @Param("tokens") Long tokens
                );

        // Reset quota for a specific user
        @Modifying
        @Query("UPDATE UserTokenQuota q SET q.currentMonthlyUsage = 0, " +
                        "q.resetDate = :newResetDate WHERE q.userId = :userId")
        int resetQuota(
                        @Param("userId") Long userId,
                        @Param("newResetDate") Instant newResetDate);

        // Find users by tier
        List<UserTokenQuota> findByTier(String tier);

        // Find users exceeding a certain usage percentage
        @Query("SELECT q FROM UserTokenQuota q WHERE " +
                        "(CAST(q.currentMonthlyUsage AS double) / CAST(q.monthlyLimit AS double)) >= :threshold")
        List<UserTokenQuota> findUsersExceedingThreshold(@Param("threshold") double threshold);
}
