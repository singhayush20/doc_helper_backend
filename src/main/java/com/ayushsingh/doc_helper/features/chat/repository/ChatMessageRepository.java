package com.ayushsingh.doc_helper.features.chat.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.ayushsingh.doc_helper.features.chat.entity.ChatMessage;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findByThreadId(String threadId, Pageable pageable);
}
