package com.ayushsingh.doc_helper.features.doc_util.service_impl;

import com.ayushsingh.doc_helper.features.doc_util.EmbeddingService;
import com.ayushsingh.doc_helper.features.user_doc.entity.DocumentStatus;
import com.ayushsingh.doc_helper.features.user_doc.entity.UserDoc;
import com.ayushsingh.doc_helper.features.user_doc.repository.UserDocRepository;
import com.ayushsingh.doc_helper.features.user_doc.service.UserDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {
    private final VectorStore vectorStore;
    private final UserDocRepository userDocRepository;

    public EmbeddingServiceImpl(VectorStore vectorStore, @Value("${storage" +
                                                                ".location}") String storageLocation, UserDocService userDocService,
            UserDocRepository userDocRepository) {
        this.vectorStore = vectorStore;
        this.userDocRepository = userDocRepository;
    }

    @Override
    @Async
    public void generateAndStoreEmbeddings(Long documentId, Long userId,
            Resource file) {
        log.info("Starting embedding process for document ID: {}", documentId);

        // 1. Find the document and update its status to PROCESSING
        UserDoc userDoc = userDocRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found for embedding: " + documentId));

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

            chunks.forEach(chunk -> {
                chunk.getMetadata().put("userId", userId);
                chunk.getMetadata().put("documentId",documentId);
                chunk.getMetadata().put("fileName", userDoc.getFileName());
            });

            vectorStore.add(chunks);

            userDoc.setStatus(DocumentStatus.READY);
            userDocRepository.save(userDoc);
            log.info("Successfully processed and embedded document ID: {}", documentId);
        }
        catch(Exception e) {
            log.error("Error while generating embeddings for document ID: {}", documentId, e);
            userDoc.setStatus(DocumentStatus.FAILED);
            userDocRepository.save(userDoc);
        }
    }
}
