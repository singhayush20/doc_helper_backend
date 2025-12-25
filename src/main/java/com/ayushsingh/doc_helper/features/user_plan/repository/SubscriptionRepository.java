package com.ayushsingh.doc_helper.features.user_plan.repository;

import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user_plan.entity.Subscription;
import com.ayushsingh.doc_helper.features.user_plan.entity.SubscriptionStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Webhook → Update subscription from Razorpay
    Optional<Subscription> findByProviderSubscriptionId(String providerSubscriptionId);

    // Get latest active subscription for user
    Optional<Subscription> findFirstByUserAndStatusInOrderByCreatedAtDesc(
            User user,
            List<SubscriptionStatus> statuses);

    // Get all user subscriptions (for history)
    List<Subscription> findByUserOrderByCreatedAtDesc(User user);
}
