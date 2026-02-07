package com.ayushsingh.doc_helper.features.doc_summary.repository;

import com.ayushsingh.doc_helper.features.doc_summary.entity.DocumentSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentSummaryRepository extends JpaRepository<DocumentSummary, Long> {

    @Query("select max(ds.versionNumber) from DocumentSummary ds where ds.documentId = :documentId")
    Integer findMaxVersionNumber(@Param("documentId") Long documentId);

    List<DocumentSummary> findByDocumentIdOrderByVersionNumberAsc(Long documentId);
}
