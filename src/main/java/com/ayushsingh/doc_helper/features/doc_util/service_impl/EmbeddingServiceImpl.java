package com.ayushsingh.doc_helper.features.doc_util.service_impl;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.doc_util.EmbeddingService;
import com.ayushsingh.doc_helper.features.user_doc.entity.DocumentStatus;
import com.ayushsingh.doc_helper.features.user_doc.entity.UserDoc;
import com.ayushsingh.doc_helper.features.user_doc.repository.UserDocRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {
    private final VectorStore vectorStore;
    private final UserDocRepository userDocRepository;

    public EmbeddingServiceImpl(VectorStore vectorStore,
            UserDocRepository userDocRepository) {
        this.vectorStore = vectorStore;
        this.userDocRepository = userDocRepository;
    }

    @Async
    @Override
    public void generateAndStoreEmbeddings(Long documentId, Long userId,
            Resource file) {
        log.info("Starting embedding process for document ID: {}", documentId);

        UserDoc userDoc = userDocRepository.findById(documentId)
                .orElseThrow(() -> new BaseException("Document not found for embedding: " + documentId,
                        ExceptionCodes.DOCUMENT_NOT_FOUND));

        if (userDoc.getStatus() != DocumentStatus.UPLOADED) {
            log.warn("Document {} is not in UPLOADED state. Aborting.",
                    documentId);
            return;
        }

        userDoc.setStatus(DocumentStatus.PROCESSING);
        userDocRepository.save(userDoc);

        try {
            TikaDocumentReader documentReader = new TikaDocumentReader(file);
            List<Document> documents = documentReader.get();

            TokenTextSplitter textSplitter = new TokenTextSplitter();
            List<Document> chunks = textSplitter.apply(documents);
            log.info("Document split into {} chunks.", chunks.size());

            // Log metadata and content for debugging
            chunks.forEach(chunk -> {
                chunk.getMetadata().put("userId", userId);
                chunk.getMetadata().put("documentId", documentId);
                chunk.getMetadata().put("fileName", userDoc.getFileName());
            });

            vectorStore.add(chunks);

            userDoc.setStatus(DocumentStatus.READY);
            userDocRepository.save(userDoc);
            log.info("Successfully processed and embedded document ID: {}", documentId);
        } catch (Exception e) {
            log.error("Error while generating embeddings for document ID: {}", documentId, e);
            userDoc.setStatus(DocumentStatus.FAILED);
            userDocRepository.save(userDoc);
        }
    }

    @Override
    public void deleteEmbeddingsByDocumentId(Long documentId) {
        try {
            String filterExpression = String.format("documentId == %d", documentId);
            vectorStore.delete(filterExpression);

            log.debug("Deleted vectors with {}={}", "documentId", documentId);
        } catch (Exception e) {
            log.error("Error deleting vectors by metadata {}={}: {}",
                    documentId, documentId, e.getMessage());
            throw new BaseException("Error deleting history",
                    ExceptionCodes.ERROR_DELETING_VECTORS);
        }
    }
}
