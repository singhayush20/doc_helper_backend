package com.ayushsingh.doc_helper.features.user_doc.service.service_impl;

import com.ayushsingh.doc_helper.config.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.doc_util.DocService;
import com.ayushsingh.doc_helper.features.user_doc.dto.FileUploadResponse;
import com.ayushsingh.doc_helper.features.user_doc.entity.DocumentStatus;
import com.ayushsingh.doc_helper.features.user_doc.entity.UserDoc;
import com.ayushsingh.doc_helper.features.user_doc.repository.UserDocRepository;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails;
import com.ayushsingh.doc_helper.features.user_doc.service.UserDocService;

import jakarta.transaction.Transactional;

@Service
public class UserDocServiceImpl implements UserDocService {

    private final DocService docService;
    private final UserDocRepository userDocRepository;

    public UserDocServiceImpl(DocService docService, UserDocRepository userDocRepository) {
        this.docService = docService;
        this.userDocRepository = userDocRepository;
    }

    @Override
    @Transactional
    public FileUploadResponse uploadDocument(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && !contentType.equals("text/plain"))) {
            throw new BaseException("Wrong file format! Only .pdf and .txt are allowed.",
                    ExceptionCodes.WRONG_FILE_FORMAT);
        }
        final var authUser = UserContext.getCurrentUser();
        final var filePath = docService.saveFile(file);

        UserDoc newUserDoc = new UserDoc();
        newUserDoc.setUser(authUser.getUser());
        newUserDoc.setStoragePath(filePath);
        newUserDoc.setFileName(file.getName());
        newUserDoc.setStatus(DocumentStatus.UPLOADED);
        var savedFile = userDocRepository.save(newUserDoc);
        return new FileUploadResponse(savedFile.getStoragePath(), savedFile.getFileName());
    }

    @Override
    public Page<UserDocDetails> getUserDocuments(Pageable pageable) {
        final var authUser = UserContext.getCurrentUser();
        return userDocRepository.findDocsByUserId(authUser.getUser().getId(), pageable);
    }

}
