package com.ayushsingh.doc_helper.features.chat.controller;

import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatHistoryResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chatbot")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(path = "/doc-question/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getStreamResponse(@RequestBody ChatRequest request) {
        return chatService.generateStreamingResponse(request);
    }

    @PostMapping(path = "/doc-question")
    public ResponseEntity<ChatCallResponse> getCallResponse(
            @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.generateResponse(request));
    }

    @GetMapping("/chat-history")
    public ResponseEntity<ChatHistoryResponse> getChatMessagesForDocument(
            @RequestParam() Long documentId,
            @RequestParam(defaultValue = "0") Integer page) {
        final var chatHistory = chatService.fetchChatHistoryForDocument(
                documentId, page);
        return ResponseEntity.ok(chatHistory);
    }
}
