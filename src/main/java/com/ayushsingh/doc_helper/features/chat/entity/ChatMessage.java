package com.ayushsingh.doc_helper.features.chat.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "chat_messages")
@Getter
@Setter
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String threadId;

    // linear position of this turn in the thread
    private Long turnNumber;

    private MessageRole role;

    private String content;

    private List<ChatResponseCitation> citations;

    @CreatedDate
    private Instant timestamp;
}
