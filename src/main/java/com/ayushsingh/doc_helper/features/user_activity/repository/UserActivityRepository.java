package com.ayushsingh.doc_helper.features.user_activity.repository;

import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserActivityRepository
        extends JpaRepository<UserActivity, Long> {

    Optional<UserActivity> findByUserIdAndDocumentId(
            Long userId,
            Long documentId
    );

    List<UserActivity> findAllByUserIdAndDocumentIdIn(
            Long userId,
            Collection<Long> documentIds
    );

    List<UserActivity> findTop5ByUserIdOrderByDominantAtDesc(
            Long userId
    );
}

