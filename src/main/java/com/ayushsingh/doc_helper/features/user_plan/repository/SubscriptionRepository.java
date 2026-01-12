package com.ayushsingh.doc_helper.features.user_plan.repository;

import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user_plan.entity.Subscription;
import com.ayushsingh.doc_helper.features.user_plan.entity.SubscriptionStatus;
import com.ayushsingh.doc_helper.features.user_plan.projection.CurrentSubscriptionView;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

        // Webhook → Update subscription from Razorpay
        Optional<Subscription> findByProviderSubscriptionId(String providerSubscriptionId);

        Optional<Subscription> findFirstByUserAndStatusInOrderByCreatedAtDesc(
                        User user,
                        List<SubscriptionStatus> statuses);

        @Query("""
                            SELECT
                                p.code AS planCode,
                                p.displayName AS planName,
                                p.tier AS planTier,
                                p.monthlyTokenLimit AS planMonthlyTokenLimit,
                                bp.priceCode AS priceCode,
                                bp.billingPeriod AS billingPeriod,
                                bp.amount AS amount,
                                bp.currency AS currency,
                                bp.description AS priceDescription,
                                s.status AS status,
                                s.cancelAtPeriodEnd AS cancelAtPeriodEnd,
                                s.currentPeriodStart AS currentPeriodStart,
                                s.currentPeriodEnd AS currentPeriodEnd
                            FROM Subscription s
                                JOIN s.billingPrice bp
                                JOIN bp.product p
                            WHERE s.user.id = :userId
                              AND s.status IN :activeStatuses
                            ORDER BY s.createdAt DESC
                        """)
        List<CurrentSubscriptionView> findCurrentSubscriptionView(
                        @Param("userId") Long userId,
                        @Param("activeStatuses") List<SubscriptionStatus> activeStatuses,
                        Pageable pageable);

        // Get all user subscriptions (for history)
        List<Subscription> findByUserOrderByCreatedAtDesc(User user);

        @Query("""
                            SELECT s
                            FROM Subscription s
                            WHERE s.status = 'CANCELED'
                              AND s.cancelAtPeriodEnd = true
                              AND s.currentPeriodEnd <= :now
                        """)
        List<Subscription> findCancelledSubscriptionsReadyForFallback(
                        @Param("now") Instant now);

        @Query("""
                            SELECT COUNT(s) > 0
                            FROM Subscription s
                            WHERE s.billingPrice.product.id = :productId
                              AND s.status IN :statuses
                        """)
        boolean existsSubscriptionsForProductWithStatuses(
                        @Param("productId") Long productId,
                        @Param("statuses") List<SubscriptionStatus> statuses);

        @Query("""
                            SELECT COUNT(s) > 0
                            FROM Subscription s
                            WHERE s.billingPrice.id = :id
                              AND s.status IN :statuses
                        """)
        boolean existsSubscriptionsForPriceWithStatuses(
                        @Param("id") Long id,
                        @Param("statuses") List<SubscriptionStatus> statuses);

        // Used by cron for reconciliation
        @Query("""
                            SELECT s
                            FROM Subscription s
                            WHERE s.status = :status
                        """)
        List<Subscription> findByStatus(@Param("status") SubscriptionStatus status);

        // Used to block new checkout
        @Query("""
                            SELECT COUNT(s) > 0
                            FROM Subscription s
                            WHERE s.user = :user
                              AND (
                                   s.status IN ('ACTIVE', 'PAST_DUE')
                                   OR (
                                        s.status = 'INCOMPLETE'
                                        AND s.checkoutExpiresAt > :now
                                   )
                              )
                        """)
        boolean existsBlockingSubscription(
                        @Param("user") User user,
                        @Param("now") Instant now);

}
