package com.ayushsingh.doc_helper.features.chat.dto;

import com.ayushsingh.doc_helper.features.chat.entity.ChatResponseCitation;
import com.ayushsingh.doc_helper.features.chat.entity.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private final String id;
    private final String content;
    private final MessageRole role;
    private final Instant timestamp;
    private final List<ChatResponseCitation> citations;
}
