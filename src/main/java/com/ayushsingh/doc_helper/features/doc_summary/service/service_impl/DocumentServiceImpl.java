package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.doc_summary.entity.Document;
import com.ayushsingh.doc_helper.features.doc_summary.repository.DocumentRepository;
import com.ayushsingh.doc_helper.features.doc_summary.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "application/octet-stream"
    );

    private final DocumentRepository documentRepository;

    @Override
    public Document createFromUpload(Long userId, MultipartFile file) {
        validateFile(file);
        String text = extractText(file);

        Document doc = new Document();
        doc.setUserId(userId);
        doc.setContentText(text);
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
                    "Wrong file format! Only .pdf and .txt are allowed.",
                    ExceptionCodes.WRONG_FILE_FORMAT
            );
        }
    }

    private String extractText(MultipartFile file) {
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes());
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<org.springframework.ai.document.Document> documents = reader.get();
            if (documents.isEmpty()) {
                throw new BaseException(
                        "Failed to parse document",
                        ExceptionCodes.DOCUMENT_PARSING_FAILED
                );
            }
            return documents.stream()
                    .map(org.springframework.ai.document.Document::getFormattedContent)
                    .filter(s -> s != null && !s.isBlank())
                    .reduce("", (a, b) -> a + "\n" + b)
                    .trim();
        } catch (Exception e) {
            log.error("Failed to parse document", e);
            throw new BaseException(
                    "Failed to parse document",
                    ExceptionCodes.DOCUMENT_PARSING_FAILED
            );
        }
    }
}
