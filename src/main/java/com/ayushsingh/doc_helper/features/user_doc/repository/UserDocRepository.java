package com.ayushsingh.doc_helper.features.user_doc.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ayushsingh.doc_helper.features.user_doc.entity.UserDoc;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails;

public interface UserDocRepository extends JpaRepository<UserDoc, Long> {

    @Query("""
                SELECT new com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails(
                    d.id,
                    d.fileName,
                    d.originalFilename,
                    d.status
                )
                FROM UserDoc d
                WHERE d.user.id = :userId
            """)
    Page<UserDocDetails> findDocsByUserId(Long userId, Pageable pageable);

    @Query("DELETE FROM UserDoc d WHERE d.id = :docId AND d.user.id = :userId")
    @Modifying
    int deleteDocument(Long docId, Long userId);

    @Query("SELECT d.fileName FROM UserDoc d WHERE d.id = :docId")
    Optional<String> findFileNameById(Long docId);
}
