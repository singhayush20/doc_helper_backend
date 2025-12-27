package com.ayushsingh.doc_helper.features.chat.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ayushsingh.doc_helper.core.ai.ChatCancellationRegistry;
import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatHistoryResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chatbot")
@Slf4j
public class ChatController {
        private final ChatService chatService;
        private final ChatCancellationRegistry chatCancellationRegistry;

        public ChatController(ChatService chatService, ChatCancellationRegistry chatCancellationRegistry) {
                this.chatService = chatService;
                this.chatCancellationRegistry = chatCancellationRegistry;
        }

        private enum MessageEvent {
                MESSAGE,
                DONE,
                ERROR
        }

        @PostMapping(path = "/doc-question/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<ChatCallResponse>> getStreamResponse(
                        @RequestParam(name = "webSearch", defaultValue = "false") Boolean webSearch,
                        @RequestParam(name = "generationId") String generationId,
                        @RequestBody ChatRequest request) {

                Flux<ServerSentEvent<ChatCallResponse>> messageEvents = chatService
                                .generateStreamingResponse(request, webSearch, generationId)
                                .map(chunk -> buildSse(MessageEvent.MESSAGE.toString(),
                                                new ChatCallResponse(chunk, null, null)));

                ServerSentEvent<ChatCallResponse> doneEvent = buildSse(MessageEvent.DONE.toString(),
                                new ChatCallResponse());

                return messageEvents
                                .concatWith(Flux.just(doneEvent))
                                .onErrorResume(ex -> Flux.just(
                                                buildSse(MessageEvent.ERROR.toString(),
                                                                buildSafeErrorResponse(ex, generationId))));
        }

        private ServerSentEvent<ChatCallResponse> buildSse(
                        String event,
                        ChatCallResponse data) {
                return ServerSentEvent.<ChatCallResponse>builder()
                                .id(UUID.randomUUID().toString())
                                .event(event)
                                .data(data)
                                .build();
        }

        private ChatCallResponse buildSafeErrorResponse(Throwable ex, String generationId) {
                log.error("Error occured in response stream for generation id: {} error: {}", generationId,
                                ex.getMessage());
                String message = "An error occurred while processing your request.";
                if (ex instanceof BaseException) {
                        message = ((BaseException) ex).getMessage();
                        var code = ((BaseException) ex).getCode();
                        return ChatCallResponse.builder().errorMessage(message).errorCode(code).build();
                }
                return ChatCallResponse.builder().errorMessage(message)
                                .errorCode(ExceptionCodes.UNKNOWN_MESSAGE_STREAM_ERROR).build();
        }

        @PostMapping(path = "/doc-question/stream/cancel")
        public ResponseEntity<Void> cancelStream(
                        @RequestParam("generationId") String generationId) {
                chatCancellationRegistry.cancel(generationId);
                return ResponseEntity.accepted().build();
        }

        @PostMapping(path = "/doc-question")
        public ResponseEntity<ChatCallResponse> getCallResponse(
                        @RequestParam(name = "webSearch", defaultValue = "false") Boolean webSearch,
                        @RequestBody ChatRequest request) {
                return ResponseEntity.ok(chatService.generateResponse(request, webSearch));
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
