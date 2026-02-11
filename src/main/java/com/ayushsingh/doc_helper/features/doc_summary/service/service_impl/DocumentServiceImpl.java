package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.doc_summary.entity.Document;
import com.ayushsingh.doc_helper.features.doc_summary.repository.DocumentRepository;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentService;
import com.ayushsingh.doc_helper.features.doc_util.DocService;
import com.ayushsingh.doc_helper.features.doc_util.dto.DocSaveResponse;
import com.ayushsingh.doc_helper.features.user_doc.entity.DocumentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "application/octet-stream",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final DocumentRepository documentRepository;
    private final DocService docService;

    @Override
    public Document uploadDocument(Long userId, MultipartFile file) {
        validateFile(file);
        DocSaveResponse saved = docService.saveFile(file);

        Document doc = new Document();
        doc.setUserId(userId);
        doc.setFileName(saved.storedFileName());
        doc.setOriginalFilename(saved.originalFileName());
        doc.setStatus(DocumentStatus.UPLOADED);
        return documentRepository.save(doc);
    }

    @Override
    public Document getByIdForUser(Long documentId, Long userId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new BaseException(
                        "Document not found",
                        ExceptionCodes.DOCUMENT_NOT_FOUND));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(
                    "Empty file",
                    ExceptionCodes.EMPTY_FILE
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BaseException(
                    "Wrong file format! Only .pdf, .txt, and .docx are allowed.",
                    ExceptionCodes.WRONG_FILE_FORMAT
            );
        }
    }

    @Override
    public boolean existsByIdAndUserId(Long documentId, Long userId) {
        return documentRepository.existsByIdAndUserId(documentId, userId);
    }

}
