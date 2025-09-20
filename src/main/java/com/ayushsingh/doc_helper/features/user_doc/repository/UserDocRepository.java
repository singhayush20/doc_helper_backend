package com.ayushsingh.doc_helper.features.user_doc.repository;

import java.util.Optional;

import com.ayushsingh.doc_helper.features.user_doc.entity.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ayushsingh.doc_helper.features.user_doc.entity.UserDoc;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails;

public interface UserDocRepository extends JpaRepository<UserDoc, Long> {

    Optional<UserDoc> findByIdAndUserId(Long documentId, Long userId);

    @Query("""
                SELECT new UserDocDetails(
                    d.id,
                    d.fileName,
                    d.storagePath,
                    d.status
                )
                FROM UserDoc d
                WHERE d.user.id = :userId
            """)
    Page<UserDocDetails> findDocsByUserId(Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE UserDoc d SET d.status = :status WHERE d.id = :docId")
    void updateDocumentProcessingStatus(Long docId, DocumentStatus status);
}
