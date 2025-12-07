package com.ayushsingh.doc_helper.user_plan.repository;

import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.user_plan.entity.Subscription;
import com.ayushsingh.doc_helper.user_plan.entity.SubscriptionStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByProviderSubscriptionId(String providerSubscriptionId);

    Optional<Subscription> findFirstByUserAndStatusInOrderByCreatedAtDesc(
            User user,
            List<SubscriptionStatus> statuses);
}
