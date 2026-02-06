package com.ayushsingh.doc_helper.features.doc_summary.service;

import com.ayushsingh.doc_helper.features.doc_summary.entity.Document;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    Document createFromUpload(Long userId, MultipartFile file);
    Document getByIdForUser(Long documentId, Long userId);
}
