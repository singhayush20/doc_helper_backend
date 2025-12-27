package com.ayushsingh.doc_helper.features.chat.repository;

import com.ayushsingh.doc_helper.features.chat.entity.ChatThread;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatThreadRepository extends
        MongoRepository<ChatThread, String> {
    Optional<ChatThread> findByDocumentIdAndUserId(Long documentId, Long userId);
}
