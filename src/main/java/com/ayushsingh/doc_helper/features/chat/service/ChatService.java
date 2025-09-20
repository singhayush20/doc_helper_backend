package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import reactor.core.publisher.Flux;

public interface ChatService {

     Flux<String> generateStreamingResponse(ChatRequest documentId);
}
