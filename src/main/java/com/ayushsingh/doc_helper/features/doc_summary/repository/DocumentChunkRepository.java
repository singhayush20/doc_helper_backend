package com.ayushsingh.doc_helper.features.doc_summary.repository;

import com.ayushsingh.doc_helper.features.doc_summary.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    boolean existsByDocumentId(Long documentId);
}
