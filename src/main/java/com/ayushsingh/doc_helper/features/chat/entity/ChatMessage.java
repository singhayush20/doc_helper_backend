package com.ayushsingh.doc_helper.features.chat.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_messages")
@Getter
@Setter
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String threadId;

    private MessageRole role;

    private String content;

    @CreatedDate
    private Instant timestamp;
}
