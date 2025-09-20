package com.ayushsingh.doc_helper.features.chat.entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@CompoundIndex(name = "user_doc_idx", def = "{'userId': 1, 'documentId': 1}", unique = true)
@Document(collection = "chat_threads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatThread {

    @Id
    private String id;

    private Long documentId;

    private Long userId;

    private String lastMessageSnippet;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private List<ChatMessage> messages = new ArrayList<>();
}
