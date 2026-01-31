package com.ayushsingh.doc_helper.features.user_doc.service;

import com.ayushsingh.doc_helper.features.user_doc.dto.FileDeletionVerificationResponse;
import com.ayushsingh.doc_helper.features.user_doc.dto.FileUploadResponse;
import com.ayushsingh.doc_helper.features.user_doc.dto.UserDocDetailsListDto;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocNameProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

public interface UserDocService {

    FileUploadResponse uploadDocument(MultipartFile file);

    UserDocDetailsListDto getUserDocuments(Pageable pageable);

    FileDeletionVerificationResponse deleteDocument(Long documentId);

    UserDocDetailsListDto searchUserDocuments(String query, Pageable pageable);

    List<UserDocNameProjection> findAllDocNamesByIdIn(Collection<Long> ids);
}
