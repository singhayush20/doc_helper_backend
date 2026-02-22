package com.ayushsingh.doc_helper.features.user_activity.repository;

import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserActivityRepository
        extends JpaRepository<UserActivity, Long> {

    Optional<UserActivity> findByUserIdAndTargetTypeAndTargetId(
            Long userId,
            ActivityTargetType targetType,
            Long targetId
    );

    List<UserActivity> findAllByUserIdAndTargetTypeAndTargetIdIn(
            Long userId,
            ActivityTargetType targetType,
            Collection<Long> targetIds
    );

    List<UserActivity> findTop5ByUserIdOrderByDominantAtDesc(
            Long userId
    );
}
