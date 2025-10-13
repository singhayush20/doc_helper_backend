package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatHistoryResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;

import reactor.core.publisher.Flux;

public interface ChatService {

     Flux<String> generateStreamingResponse(ChatRequest chatRequest);
     ChatCallResponse generateResponse(ChatRequest chatRequest);

     ChatHistoryResponse fetchChatHistoryForDocument(Long documentId,
             Integer page);

     Boolean deleteChatHistoryForDocument(Long documentId);
}
