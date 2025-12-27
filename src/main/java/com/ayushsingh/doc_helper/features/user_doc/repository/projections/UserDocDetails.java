package com.ayushsingh.doc_helper.features.user_doc.repository.projections;

import com.ayushsingh.doc_helper.features.user_doc.entity.DocumentStatus;

public record UserDocDetails(Long id, String fileName, String originalFilename, DocumentStatus status) {

}
