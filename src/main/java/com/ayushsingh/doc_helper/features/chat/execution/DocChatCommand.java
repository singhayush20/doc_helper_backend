package com.ayushsingh.doc_helper.features.chat.execution;

import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;

public record DocChatCommand(ChatRequest request, boolean webSearch) {
}
