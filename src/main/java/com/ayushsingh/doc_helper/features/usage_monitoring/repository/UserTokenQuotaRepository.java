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
   * Find quotas that need to be reset with pagination
   * Used by QuotaResetScheduler.
   */
  @Query("""
      SELECT q
      FROM UserTokenQuota q
      WHERE q.resetDate <= :now
        AND q.isActive = true
      """)
  Page<UserTokenQuota> findQuotasToResetPaginated(@Param("now") Instant now, Pageable pageable);

  /**
   * Atomic increment of currentMonthlyUsage for a user within current billing
   * period.
   * Returns number of rows updated (0 = user inactive or quota row not in current
   * period).
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE UserTokenQuota q
      SET q.currentMonthlyUsage = q.currentMonthlyUsage + :tokensToUse
      WHERE q.userId = :userId
        AND q.isActive = true
        AND q.resetDate > :now
      """)
  int incrementUsageIfActiveAndInPeriod(@Param("userId") Long userId,
      @Param("tokensToUse") Long tokensToUse,
      @Param("now") Instant now);

  /**
   * Reset quota for given user if reset is due.
   * Used by QuotaResetScheduler via service.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE UserTokenQuota q
      SET q.currentMonthlyUsage = 0,
          q.resetDate = :newResetDate
      WHERE q.userId = :userId
        AND q.resetDate <= :now
      """)
  int resetQuota(@Param("userId") Long userId,
      @Param("now") Instant now,
      @Param("newResetDate") Instant newResetDate);

  /**
   * Admin-only or exceptional manual adjustment.
   * Do NOT use this for normal request-path quota consumption.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE UserTokenQuota q
      SET q.currentMonthlyUsage = q.currentMonthlyUsage + :tokens
      WHERE q.userId = :userId
      """)
  void incrementUsage(@Param("userId") Long userId,
      @Param("tokens") Long tokens);
}
