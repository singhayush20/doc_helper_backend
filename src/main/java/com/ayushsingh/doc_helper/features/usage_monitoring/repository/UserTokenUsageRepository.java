package com.ayushsingh.doc_helper.features.usage_monitoring.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;

public interface UserTokenUsageRepository extends JpaRepository<UserTokenUsage, Long> {

        // Find usage by user with pagination
        Page<UserTokenUsage> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

        // Find usage for a specific document
        Page<UserTokenUsage> findByUserIdAndDocumentIdOrderByTimestampDesc(
                        Long userId, Long documentId, Pageable pageable);

        // Find usage within date range
        @Query("SELECT u FROM UserTokenUsage u WHERE u.userId = :userId " +
                        "AND u.timestamp BETWEEN :startDate AND :endDate " +
                        "ORDER BY u.timestamp DESC")
        List<UserTokenUsage> findByUserIdAndTimestampBetween(
                        @Param("userId") Long userId,
                        @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);

        // Sum total tokens for a user in current month
        @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM UserTokenUsage u " +
                        "WHERE u.userId = :userId AND u.timestamp >= :startOfMonth")
        Long sumTokensForUserSince(
                        @Param("userId") Long userId,
                        @Param("startOfMonth") Instant startOfMonth);

        // Sum estimated cost for a user
        @Query("SELECT COALESCE(SUM(u.estimatedCost), 0) FROM UserTokenUsage u " +
                        "WHERE u.userId = :userId AND u.timestamp >= :startDate")
        BigDecimal sumCostForUserSince(
                        @Param("userId") Long userId,
                        @Param("startDate") Instant startDate);

        // Get daily usage summary
        @Query("SELECT DATE(u.timestamp) as date, " +
                        "SUM(u.totalTokens) as totalTokens, " +
                        "SUM(u.estimatedCost) as totalCost, " +
                        "COUNT(u) as requestCount " +
                        "FROM UserTokenUsage u " +
                        "WHERE u.userId = :userId " +
                        "AND u.timestamp BETWEEN :startDate AND :endDate " +
                        "GROUP BY DATE(u.timestamp) " +
                        "ORDER BY DATE(u.timestamp) DESC")
        List<Object[]> getDailyUsageSummary(
                        @Param("userId") Long userId,
                        @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);

        // Count requests by operation type
        @Query("SELECT u.operationType, COUNT(u) FROM UserTokenUsage u " +
                        "WHERE u.userId = :userId " +
                        "GROUP BY u.operationType")
        List<Object[]> countByOperationType(@Param("userId") Long userId);

        // Get usage for a specific thread
        List<UserTokenUsage> findByThreadIdOrderByTimestampAsc(String threadId);

        /**
         * Get usage breakdown by operation type
         */
        @Query("SELECT u.operationType, " +
                        "COUNT(u), " +
                        "SUM(u.totalTokens), " +
                        "SUM(u.estimatedCost) " +
                        "FROM UserTokenUsage u " +
                        "WHERE u.userId = :userId " +
                        "GROUP BY u.operationType")
        List<Object[]> getUsageBreakdownByOperationType(@Param("userId") Long userId);

        /**
         * Get usage breakdown by operation type within date range
         */
        @Query("SELECT u.operationType, " +
                        "COUNT(u), " +
                        "SUM(u.totalTokens), " +
                        "SUM(u.estimatedCost) " +
                        "FROM UserTokenUsage u " +
                        "WHERE u.userId = :userId " +
                        "AND u.timestamp BETWEEN :startDate AND :endDate " +
                        "GROUP BY u.operationType")
        List<Object[]> getUsageBreakdownByOperationTypeAndDateRange(
                        @Param("userId") Long userId,
                        @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);
}
