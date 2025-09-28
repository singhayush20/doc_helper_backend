package com.ayushsingh.doc_helper.features.user_doc.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.ayushsingh.doc_helper.features.user_doc.dto.FileUploadResponse;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails;

public interface UserDocService {

    FileUploadResponse uploadDocument(MultipartFile file);

    Page<UserDocDetails> getUserDocuments(Pageable pageable);

    Boolean deleteDocument(Long documentId);
}
