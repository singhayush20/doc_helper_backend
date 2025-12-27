package com.ayushsingh.doc_helper.features.usage_monitoring.dto;

import com.ayushsingh.doc_helper.features.chat.entity.ChatThread;
import org.springframework.ai.chat.prompt.Prompt;

public record ChatContext(
        Long documentId,
        Long userId,
        ChatThread chatThread,
        Prompt prompt,
        String ragContext,
        String historyContext
) {}
