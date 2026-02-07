package com.ayushsingh.doc_helper.features.doc_summary.controller;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.doc_summary.dto.DocumentUploadResponseDto;
import com.ayushsingh.doc_helper.features.doc_summary.entity.Document;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentUploadResponseDto> uploadDocument(
            @RequestParam("file") MultipartFile file
    ) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        Document doc = documentService.uploadDocument(userId, file);
        return ResponseEntity.ok(
                DocumentUploadResponseDto.builder()
                        .documentId(doc.getId())
                        .build()
        );
    }
}
