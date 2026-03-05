package com.ayushsingh.doc_helper.features.doc_summary.controller;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.doc_summary.dto.DocumentDetailsDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.DocumentListResponse;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentService;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentSummaryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentSummaryService documentSummaryService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentDetailsDto> uploadDocument(
            @RequestParam("file") MultipartFile file
    ) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        
        return ResponseEntity.ok( documentService.uploadDocument(userId, file));
       
    }

    @GetMapping
    public ResponseEntity<DocumentListResponse> getDocuments() {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        return ResponseEntity.ok(documentService.getDocumentsForUser(userId));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        documentService.deleteDocument(userId, documentId);
        documentSummaryService.deleteDocumentSummariesAsync(documentId);
        return ResponseEntity.ok().build();
    }
}
