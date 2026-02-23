package com.ayushsingh.doc_helper.features.doc_summary.service;

import com.ayushsingh.doc_helper.features.doc_summary.dto.DocumentDetailsDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.DocumentListResponse;
import com.ayushsingh.doc_helper.features.doc_summary.entity.Document;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    
    DocumentDetailsDto uploadDocument(Long userId, MultipartFile file);

    Document getByIdForUser(Long documentId, Long userId);

    boolean existsByIdAndUserId(Long documentId, Long userId);

    DocumentListResponse getDocumentsForUser(Long userId);

    void deleteDocument(Long userId, Long documentId);
}
