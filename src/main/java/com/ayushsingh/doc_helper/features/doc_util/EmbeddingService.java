package com.ayushsingh.doc_helper.features.doc_util;

import org.springframework.core.io.Resource;

public interface EmbeddingService {

    void generateAndStoreEmbeddings(Long docId, Long userId, Resource file);
    void deleteEmbeddingsByDocumentId(Long documentId);    
    Long estimateEmbeddingTokens(Resource file);

}
