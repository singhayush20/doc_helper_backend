package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.features.chat.entity.ChatThread;

public interface ChatSummaryService {
    void summarizeThread(String threadId);

    String getCachedSummary(ChatThread thread);
}
