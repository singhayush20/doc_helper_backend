package com.ayushsingh.doc_helper.features.user_doc.service.service_impl;

import com.ayushsingh.doc_helper.config.security.UserContext;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;
import com.ayushsingh.doc_helper.features.doc_util.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.doc_util.DocService;
import com.ayushsingh.doc_helper.features.user_doc.dto.FileDeletionVerificationResponse;
import com.ayushsingh.doc_helper.features.user_doc.dto.FileUploadResponse;
import com.ayushsingh.doc_helper.features.user_doc.entity.DocumentStatus;
import com.ayushsingh.doc_helper.features.user_doc.entity.UserDoc;
import com.ayushsingh.doc_helper.features.user_doc.repository.UserDocRepository;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails;
import com.ayushsingh.doc_helper.features.user_doc.service.UserDocService;

@Slf4j
@Service
public class UserDocServiceImpl implements UserDocService {

    private final DocService docService;
    private final UserDocRepository userDocRepository;
    private final EmbeddingService embeddingService;
    private final ChatService chatService;

    public UserDocServiceImpl(DocService docService, UserDocRepository userDocRepository,
            EmbeddingService embeddingService, ChatService chatService) {
        this.docService = docService;
        this.userDocRepository = userDocRepository;
        this.embeddingService = embeddingService;
        this.chatService = chatService;
    }

    @Override
    @Transactional
    public FileUploadResponse uploadDocument(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && !contentType.equals("text/plain"))) {
            log.error("Wrong file format! Only .pdf and .txt are allowed.");
            throw new BaseException("Wrong file format! Only .pdf and .txt are allowed.",
                    ExceptionCodes.WRONG_FILE_FORMAT);
        }
        final var authUser = UserContext.getCurrentUser();
        final var fileName = docService.saveFile(file);
        log.info("File uploaded successfully...");

        UserDoc newUserDoc = new UserDoc();
        newUserDoc.setUser(authUser.getUser());
        newUserDoc.setFileName(fileName);
        newUserDoc.setStatus(DocumentStatus.UPLOADED);
        var savedFile = userDocRepository.save(newUserDoc);
        log.info("File record saved successfully...");

        Resource resource = docService.loadFileAsResource(fileName);
        embeddingService.generateAndStoreEmbeddings(savedFile.getId(), authUser.getUser().getId(), resource);

        return new FileUploadResponse(savedFile.getFileName(), savedFile.getFileName());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDocDetails> getUserDocuments(Pageable pageable) {
        final var authUser = UserContext.getCurrentUser();
        return userDocRepository.findDocsByUserId(authUser.getUser().getId(), pageable);
    }

    @Override
    @Transactional
    public FileDeletionVerificationResponse deleteDocument(Long documentId) {
        final var authUser = UserContext.getCurrentUser();
        final var sourcePath = userDocRepository.findFileNameById(documentId);
        final var affectedRows = userDocRepository.deleteDocument(documentId,
                authUser.getUser().getId());
        if (affectedRows != 0) {
            chatService.deleteChatHistoryForDocument(documentId);
            embeddingService.deleteEmbeddingsByDocumentId(documentId);
            sourcePath.ifPresent(docService::deleteFile);
        }

        return new FileDeletionVerificationResponse(affectedRows != 0);
    }
}
