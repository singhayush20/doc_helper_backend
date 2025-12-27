package com.ayushsingh.doc_helper.features.usage_monitoring.repository;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;
import com.ayushsingh.doc_helper.features.usage_monitoring.projection.DailyUsageSummaryProjection;
import com.ayushsingh.doc_helper.features.usage_monitoring.projection.UsageBreakdownProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface UserTokenUsageRepository extends
        JpaRepository<UserTokenUsage, Long> {

    // Find usage by user with pagination (using createdAt)
    Page<UserTokenUsage> findByUserIdOrderByCreatedAtDesc(Long userId,
            Pageable pageable);

    // Find usage for a specific document
    Page<UserTokenUsage> findByUserIdAndDocumentIdOrderByCreatedAtDesc(
            Long userId, Long documentId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(u.estimatedCost), 0) FROM UserTokenUsage u " +
           "WHERE u.userId = :userId AND u.createdAt >= :startDate")
    BigDecimal sumCostForUserSince(@Param("userId") Long userId,
            @Param("startDate") Instant startDate);

    /**
     * Get daily usage summary using interface projection
     */
    @Query("SELECT DATE_TRUNC('day', u.createdAt) as date, " +
           "SUM(u.totalTokens) as totalTokens, " +
           "SUM(u.estimatedCost) as totalCost, " + "COUNT(u) as requestCount " +
           "FROM UserTokenUsage u " + "WHERE u.userId = :userId " +
           "AND u.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE_TRUNC('day', u.createdAt) " +
           "ORDER BY DATE_TRUNC('day', u.createdAt) DESC")
    List<DailyUsageSummaryProjection> getDailyUsageSummary(
            @Param("userId") Long userId, @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Get usage breakdown by operation type
     */
    @Query("SELECT u.operationType as operationType, " +
           "COUNT(u) as requestCount, " +
           "SUM(u.totalTokens) as totalTokens, " +
           "SUM(u.estimatedCost) as totalCost " + "FROM UserTokenUsage u " +
           "WHERE u.userId = :userId " + "GROUP BY u.operationType")
    List<UsageBreakdownProjection> getUsageBreakdownByOperationType(
            @Param("userId") Long userId);

    /**
     * Get usage breakdown by operation type within date range
     */
    @Query("SELECT u.operationType as operationType, " +
           "COUNT(u) as requestCount, " +
           "SUM(u.totalTokens) as totalTokens, " +
           "SUM(u.estimatedCost) as totalCost " + "FROM UserTokenUsage u " +
           "WHERE u.userId = :userId " +
           "AND u.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY u.operationType")
    List<UsageBreakdownProjection> getUsageBreakdownByOperationTypeAndDateRange(
            @Param("userId") Long userId, @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
