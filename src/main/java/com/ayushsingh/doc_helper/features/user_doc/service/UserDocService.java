package com.ayushsingh.doc_helper.features.user_doc.service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.ayushsingh.doc_helper.features.user_doc.dto.FileDeletionVerificationResponse;
import com.ayushsingh.doc_helper.features.user_doc.dto.FileUploadResponse;
import com.ayushsingh.doc_helper.features.user_doc.dto.UserDocDetailsListDto;

public interface UserDocService {

    FileUploadResponse uploadDocument(MultipartFile file);

    UserDocDetailsListDto getUserDocuments(Pageable pageable);

    FileDeletionVerificationResponse deleteDocument(Long documentId);

    UserDocDetailsListDto searchUserDocuments(String query, Pageable pageable);
}
