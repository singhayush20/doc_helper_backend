package com.ayushsingh.doc_helper.features.user_doc.service;

import org.springframework.web.multipart.MultipartFile;

import com.ayushsingh.doc_helper.features.user_doc.dto.FileUploadResponse;

public interface UserDocService {

    FileUploadResponse uploadDocument(MultipartFile file);
}
