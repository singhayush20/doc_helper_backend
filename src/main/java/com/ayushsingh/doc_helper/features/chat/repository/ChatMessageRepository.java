package com.ayushsingh.doc_helper.features.chat.repository;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.ayushsingh.doc_helper.features.chat.entity.ChatMessage;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findByThreadId(String threadId, Pageable pageable);

    @Query(value = "{ 'threadId': ?0 }", sort = "{ 'timestamp': -1 }")
    List<ChatMessage> findLastNTurns(String threadId, Pageable pageable);

    default List<ChatMessage> findLastNTurns(String threadId, int count) {
        return findLastNTurns(threadId, PageRequest.of(0, count))
                .stream()
                .sorted(Comparator.comparing(ChatMessage::getTimestamp))
                .toList();
    }
}
