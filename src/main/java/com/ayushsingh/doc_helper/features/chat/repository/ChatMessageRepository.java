package com.ayushsingh.doc_helper.features.chat.repository;

import com.ayushsingh.doc_helper.features.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findByThreadId(String threadId, Pageable pageable);
}
