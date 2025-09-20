package com.ayushsingh.doc_helper.features.user_doc.controller;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ayushsingh.doc_helper.features.user_doc.dto.FileUploadResponse;
import com.ayushsingh.doc_helper.features.user_doc.entity.SortField;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails;
import com.ayushsingh.doc_helper.features.user_doc.service.UserDocService;

@RestController
@RequestMapping("/api/v1/user-docs")
public class UserDocController {

    private final UserDocService userDocService;

    public UserDocController(UserDocService userDocService) {
        this.userDocService = userDocService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadDocument(@RequestParam MultipartFile file) {
        var filePath = userDocService.uploadDocument(file);
        return ResponseEntity.ok(filePath);
    }

    @GetMapping("/all")
    public ResponseEntity<Page<UserDocDetails>> getUserDocs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "CREATED_AT") SortField sortField,
            @RequestParam(defaultValue = "asc") String direction) {

        if (size > 10) {
            throw new BaseException("Page size must not exceed 10",
                    ExceptionCodes.LARGE_PAGE_SIZE_ERROR);
        }

        // Map SortField to actual field name
        String fieldName = sortField.getFieldName();
        Sort sort = Sort.by(Sort.Direction.fromString(direction), fieldName);
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(userDocService.getUserDocuments(pageable));
    }
}
