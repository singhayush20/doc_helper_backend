package com.ayushsingh.doc_helper.features.chat.controller;

import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chatbot")
public class ChatController {
    private final ChatService chatService;

    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> handleChatMessage(
            @RequestBody ChatRequest request) {

        // A basic security check could be added here to verify that
        // the user specified by userPublicId has permission to access documentId.

        return chatService.generateStreamingResponse(
                request
        );
    }
}
