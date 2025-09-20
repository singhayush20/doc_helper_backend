package com.ayushsingh.doc_helper.features.user_doc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ayushsingh.doc_helper.features.user_doc.dto.FileUploadResponse;
import com.ayushsingh.doc_helper.features.user_doc.service.UserDocService;

@RestController
@RequestMapping("/api/v1/user-docs")
public class UserDocController {

    private final UserDocService userDocService;

    public UserDocController(UserDocService userDocService) {
        this.userDocService = userDocService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadDocument(@RequestBody MultipartFile file) {
        var filePath = userDocService.uploadDocument(file);
        return ResponseEntity.ok(filePath);
    }

}
