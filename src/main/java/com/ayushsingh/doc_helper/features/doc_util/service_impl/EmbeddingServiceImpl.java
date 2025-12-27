package com.ayushsingh.doc_helper.features.doc_util.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.doc_util.EmbeddingService;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.EmbeddingUsageService;
import com.ayushsingh.doc_helper.features.user_doc.entity.DocumentStatus;
import com.ayushsingh.doc_helper.features.user_doc.entity.UserDoc;
import com.ayushsingh.doc_helper.features.user_doc.repository.UserDocRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

        private static final String EMBEDDING_MODEL = "gemini-embedding-001";
        private static final int MAX_TOKENS_PER_CHUNK = 2048;
        private static final int DEFAULT_CHUNK_SIZE = 1024;
        private static final int MIN_CHUNK_OVERLAP_SIZE = 100;

        private final VectorStore vectorStore;
        private final UserDocRepository userDocRepository;
        private final EmbeddingUsageService embeddingUsageService;
        private final JTokkitTokenCountEstimator tokenEstimator;

        public EmbeddingServiceImpl(VectorStore vectorStore,
                        UserDocRepository userDocRepository,
                        EmbeddingUsageService embeddingUsageService) {
                this.vectorStore = vectorStore;
                this.userDocRepository = userDocRepository;
                this.embeddingUsageService = embeddingUsageService;
                this.tokenEstimator = new JTokkitTokenCountEstimator();
        }

        @Async
        @Override
        public void generateAndStoreEmbeddings(Long documentId, Long userId, Resource file) {
                log.info("Starting embedding process for document ID: {}, userId: {}", documentId, userId);

                UserDoc userDoc = userDocRepository.findById(documentId)
                                .orElseThrow(() -> new BaseException(
                                                "Document not found for id: " + documentId,
                                                ExceptionCodes.DOCUMENT_NOT_FOUND));

                if (userDoc.getStatus() != DocumentStatus.UPLOADED) {
                        log.warn("Document {} is not in UPLOADED state (current: {}). Aborting.",
                                        documentId, userDoc.getStatus());
                        return;
                }

                userDoc.setStatus(DocumentStatus.PROCESSING);
                userDocRepository.save(userDoc);

                try {
                        List<Document> chunks = readAndSplit(file, userDoc.getFileName());
                        log.info("Document {} split into {} chunks", documentId, chunks.size());

                        if (chunks.isEmpty()) {
                                log.warn("No chunks generated from document {}. Setting status to FAILED.",
                                                documentId);
                                userDoc.setStatus(DocumentStatus.FAILED);
                                userDocRepository.save(userDoc);
                                return;
                        }

                        List<String> chunkTexts = chunks.stream()
                                        .map(Document::getFormattedContent)
                                        .toList();

                        Long estimatedTokens = estimateTotalTokens(chunkTexts);

                        log.debug("Document {}: estimated {} tokens across {} chunks (avg {} tokens/chunk)",
                                        documentId, estimatedTokens, chunks.size(),
                                        chunks.isEmpty() ? 0 : estimatedTokens / chunks.size());

                        validateChunkTokens(chunkTexts);

                        try {
                                embeddingUsageService.recordEmbeddingUsage(
                                                userId,
                                                documentId,
                                                EMBEDDING_MODEL,
                                                chunks.size(),
                                                estimatedTokens);

                                log.info("Successfully recorded embedding usage for document {}: {} tokens, {} chunks",
                                                documentId, estimatedTokens, chunks.size());

                        } catch (BaseException e) {
                                log.error("Cannot process document {}: {}", documentId, e.getMessage());
                                userDoc.setStatus(DocumentStatus.FAILED);
                                userDocRepository.save(userDoc);
                                return;
                        }

                        for (int i = 0; i < chunks.size(); i++) {
                                Document chunk = chunks.get(i);
                                chunk.getMetadata().put("userId", userId);
                                chunk.getMetadata().put("documentId", documentId);
                                chunk.getMetadata().put("fileName", userDoc.getFileName());
                                chunk.getMetadata().put("chunkIndex", i);
                                chunk.getMetadata().put("totalChunks", chunks.size());

                                log.trace("Chunk {}/{}: {} chars, ~{} tokens",
                                                i + 1,
                                                chunks.size(),
                                                chunk.getFormattedContent().length(),
                                                tokenEstimator.estimate(chunk.getFormattedContent()));
                        }

                        log.info("Starting Gemini embedding generation for {} chunks", chunks.size());
                        vectorStore.add(chunks);

                        userDoc.setStatus(DocumentStatus.READY);
                        userDocRepository.save(userDoc);

                        log.info("Successfully processed and embedded document ID: {} ({} chunks, ~{} tokens)",
                                        documentId, chunks.size(), estimatedTokens);

                } catch (Exception e) {
                        log.error("Error while generating embeddings for document ID: {}", documentId, e);
                        userDoc.setStatus(DocumentStatus.FAILED);
                        userDocRepository.save(userDoc);
                }
        }

        /**
         * Used by upload layer for quota pre-check.
         */
        @Override
        public Long estimateEmbeddingTokens(Resource file) {
                try {
                        List<Document> chunks = readAndSplit(file, "<upload-preview>");
                        List<String> chunkTexts = chunks.stream()
                                        .map(Document::getFormattedContent)
                                        .toList();

                        Long estimatedTokens = estimateTotalTokens(chunkTexts);

                        log.debug("Estimated {} tokens for file during upload pre-check", estimatedTokens);
                        return estimatedTokens;
                } catch (Exception e) {
                        log.error("Failed to estimate tokens during upload pre-check", e);
                        // conservative fallback: approximate using characters if Tika fails
                        throw new BaseException(
                                        "Failed to estimate tokens for upload: " + e.getMessage(),
                                        ExceptionCodes.DOCUMENT_PARSING_FAILED);
                }
        }

        private List<Document> readAndSplit(Resource file, String fileNameForLog) {
                TikaDocumentReader documentReader = new TikaDocumentReader(file);
                List<Document> documents = documentReader.get();
                log.debug("Read {} document(s) from file: {}", documents.size(), fileNameForLog);

                TokenTextSplitter textSplitter = new TokenTextSplitter(
                                DEFAULT_CHUNK_SIZE,
                                MIN_CHUNK_OVERLAP_SIZE,
                                10,
                                MAX_TOKENS_PER_CHUNK,
                                true);

                return textSplitter.apply(documents);
        }

        public Long estimateTotalTokens(List<String> chunkTexts) {
                return chunkTexts.stream().mapToLong(text -> {
                        try {
                                return tokenEstimator.estimate(text);
                        } catch (Exception e) {
                                log.warn("Failed to estimate tokens for chunk, using character-based fallback");
                                return text.length() / 4;
                        }
                }).sum();
        }

        private void validateChunkTokens(List<String> chunkTexts) {
                for (int i = 0; i < chunkTexts.size(); i++) {
                        long tokens = tokenEstimator.estimate(chunkTexts.get(i));
                        if (tokens > MAX_TOKENS_PER_CHUNK) {
                                log.warn("Chunk {} exceeds Gemini token limit: {} > {} tokens",
                                                i, tokens, MAX_TOKENS_PER_CHUNK);
                        }
                }
        }

        @Override
        public void deleteEmbeddingsByDocumentId(Long documentId) {
                try {
                        log.info("Deleting embeddings for document ID: {}", documentId);

                        String filterExpression = String.format("documentId == %d", documentId);
                        vectorStore.delete(filterExpression);

                        log.info("Successfully deleted vectors for documentId={}", documentId);

                } catch (Exception e) {
                        log.error("Error deleting vectors for documentId={}: {}", documentId, e.getMessage(), e);
                        throw new BaseException(
                                        "Error deleting document embeddings: " + e.getMessage(),
                                        ExceptionCodes.ERROR_DELETING_VECTORS);
                }
        }
}
