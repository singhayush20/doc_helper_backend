package com.ayushsingh.doc_helper.features.chat.service.service_impl;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.config.security.UserContext;
import com.ayushsingh.doc_helper.features.chat.dto.ChatHistoryResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatMessageResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.entity.ChatMessage;
import com.ayushsingh.doc_helper.features.chat.entity.ChatThread;
import com.ayushsingh.doc_helper.features.chat.entity.MessageRole;
import com.ayushsingh.doc_helper.features.chat.repository.ChatMessageRepository;
import com.ayushsingh.doc_helper.features.chat.repository.ChatThreadRepository;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;
import com.ayushsingh.doc_helper.features.user_doc.repository.UserDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final String RAG_PROMPT_TEMPLATE = """
            You are a helpful assistant for the document.
            Use the information provided in the "CONTEXT" section and the "CHAT HISTORY" to answer the user's question.
            If the answer is not available in the context or chat history, say "I do not have information about that."
            
            CONTEXT:
            {context}
            
            CHAT HISTORY:
            {chatHistory}
            """;
    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserDocRepository userDocRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Flux<String> generateStreamingResponse(ChatRequest chatRequest) {
        final var documentId = chatRequest.documentId();
        final var userQuestion = chatRequest.question();

        if (!userDocRepository.existsById(documentId)) {
            log.error("Document not found for documentId: {}", documentId);
            throw new BaseException(
                    "Document not found for documentId: " + documentId,
                    ExceptionCodes.DOCUMENT_NOT_FOUND);
        }

        log.debug("Generating streaming response for documentId: {}",
                documentId);

        final var userId = UserContext.getCurrentUser().getUser().getId();
        ChatThread chatThread = getOrCreateChatThread(documentId, userId);

        String ragContext = retrieveRagContext(documentId, userId,
                userQuestion);
        String historyContext = retrieveHistoryContext(chatThread.getId());
        String finalPrompt = RAG_PROMPT_TEMPLATE.replace("{context}",
                ragContext).replace("{chatHistory}", historyContext);
        SystemMessage systemMessage = new SystemMessage(finalPrompt);
        UserMessage userMessage = new UserMessage(userQuestion);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        ChatClient chatClient = chatClientBuilder.build();

        StringBuilder fullResponse = new StringBuilder();

        saveUserMessage(chatThread, userQuestion);

        return chatClient.prompt(prompt)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doFinally(signalType -> {
                    if (signalType == SignalType.ON_COMPLETE &&
                        !fullResponse.isEmpty()) {
                        saveAssistantMessage(chatThread,
                                fullResponse.toString());
                    }
                });
    }

    private String retrieveRagContext(Long documentId, Long userId,
            String userQuestion) {
        log.debug("Retrieving RAG context for documentId: {} and userId: {}",
                documentId, userId);
        SearchRequest request = SearchRequest.builder()
                .query(userQuestion)
                .topK(4)
                .filterExpression("userId == " + userId + " && documentId == " +
                                  documentId)
                .build();

        List<Document> similarDocs = vectorStore.similaritySearch(request);

        if (similarDocs != null && !similarDocs.isEmpty()) {
            return similarDocs.stream()
                    .map(Document::getFormattedContent)
                    .collect(Collectors.joining("\n---\n"));
        } else {
            return "";
        }
    }

    private String retrieveHistoryContext(String threadId) {
        log.debug("Retrieving history context for threadId: {}", threadId);
        PageRequest pageRequest = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.DESC, "timestamp"));
        List<ChatMessage> recentMessages = chatMessageRepository.findByThreadId(
                threadId, pageRequest);

        return recentMessages.stream()
                .map(msg -> msg.getRole().getValue() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));
    }

    private void saveUserMessage(ChatThread thread, String userQuestion) {
        log.debug("Saving user message for threadId: {}", thread.getId());
        ChatMessage userMessage = new ChatMessage();
        userMessage.setThreadId(thread.getId());
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(userQuestion);
        userMessage.setTimestamp(Instant.now());
        chatMessageRepository.save(userMessage);

        thread.setLastMessageSnippet(
                userQuestion.length() > 50 ?
                        userQuestion.substring(0, 50) + "..." : userQuestion);
        chatThreadRepository.save(thread);
    }

    private void saveAssistantMessage(ChatThread thread, String aiResponse) {
        log.debug("Saving assistant message for threadId: {}", thread.getId());
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setThreadId(thread.getId());
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(aiResponse);
        assistantMessage.setTimestamp(Instant.now());
        chatMessageRepository.save(assistantMessage);
    }

    @Override
    public ChatHistoryResponse fetchChatHistoryForDocument(Long documentId) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        var chatThread =
                chatThreadRepository.findByDocumentIdAndUserId(documentId,
                        userId);
        if (chatThread.isPresent()) {
            PageRequest pageRequest = PageRequest.of(0, 10,
                    Sort.by(Sort.Direction.DESC, "timestamp"));
            List<ChatMessage> recentMessages =
                    chatMessageRepository.findByThreadId(
                            chatThread.get().getId(), pageRequest);

            List<ChatMessageResponse> messageResponses = recentMessages.stream()
                    .map(msg -> new ChatMessageResponse(
                            msg.getId(),
                            msg.getContent(),
                            msg.getRole(),
                            msg.getTimestamp()))
                    .collect(Collectors.toList());

            String threadId = recentMessages.isEmpty() ? null :
                    recentMessages.getFirst().getThreadId();

            return new ChatHistoryResponse(threadId, messageResponses);
        } else {
            return new ChatHistoryResponse();
        }
    }

    private ChatThread getOrCreateChatThread(Long documentId, Long userId) {
        log.debug("Getting or creating chat thread for documentId: {}",
                documentId);
        Query query = new Query(
                Criteria.where("documentId")
                        .is(documentId)
                        .and("userId")
                        .is(userId));
        Update update = new Update()
                .setOnInsert("documentId", documentId)
                .setOnInsert("userId", userId)
                .setOnInsert("createdAt", Instant.now())
                .set("updatedAt", Instant.now());

        mongoTemplate.upsert(query, update, ChatThread.class);
        return mongoTemplate.findOne(query, ChatThread.class);
    }
}
