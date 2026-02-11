package com.ayushsingh.doc_helper.features.doc_summary.repository;

import com.ayushsingh.doc_helper.features.doc_summary.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long documentId, Long userId);
}
