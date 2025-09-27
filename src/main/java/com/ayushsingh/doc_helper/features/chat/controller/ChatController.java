package com.ayushsingh.doc_helper.features.chat.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatbot")
public class ChatController {
    private final ChatService chatService;

    @PostMapping(path = "/doc-question", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> handleChatMessage(
            @RequestBody ChatRequest request) {
        return chatService.generateStreamingResponse(
                request);
    }
}
