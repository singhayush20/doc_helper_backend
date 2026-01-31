package com.ayushsingh.doc_helper.features.user_doc.repository;

import com.ayushsingh.doc_helper.features.user_doc.entity.UserDoc;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocNameProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    // Substring search (ILIKE) returning projection
    @Query("""
                SELECT new com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails(
                    d.id,
                    d.fileName,
                    d.originalFilename,
                    d.status
                )
                FROM UserDoc d
                WHERE d.user.id = :userId
                AND LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%'))
                ORDER BY d.createdAt DESC
            """)
    Page<com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails> searchByOriginalFilename(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable);

    // Fuzzy search (pg_trgm similarity) using JPQL function() to call the SQL similarity function
    @Query("""
                SELECT new com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails(
                    d.id,
                    d.fileName,
                    d.originalFilename,
                    d.status
                )
                FROM UserDoc d
                WHERE d.user.id = :userId
                AND function('similarity', d.originalFilename, :query) > 0.3
                ORDER BY function('similarity', d.originalFilename, :query) DESC
            """)
    Page<com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails> searchByOriginalFilenameFuzzy(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable);

    @Query("""
                    SELECT new com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocNameProjection(
                    d.id,
                    d.originalFilename
                    )
                    FROM UserDoc d
                    WHERE d.id IN :ids
            """)
    List<UserDocNameProjection> findAllByIdIn(Collection<Long> ids);

}
