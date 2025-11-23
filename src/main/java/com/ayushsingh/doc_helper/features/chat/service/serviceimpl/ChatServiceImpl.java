package com.ayushsingh.doc_helper.features.chat.service.serviceimpl;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.config.ai.ChatCancellationRegistry;
import com.ayushsingh.doc_helper.config.ai.advisors.LoggingAdvisor;
import com.ayushsingh.doc_helper.config.ai.prompts.PromptTemplates;
import com.ayushsingh.doc_helper.config.ai.tools.websearch.WebSearchTool;
import com.ayushsingh.doc_helper.config.security.UserContext;
import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatHistoryResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatMessageResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.entity.ChatMessage;
import com.ayushsingh.doc_helper.features.chat.entity.ChatThread;
import com.ayushsingh.doc_helper.features.chat.entity.MessageRole;
import com.ayushsingh.doc_helper.features.chat.entity.TurnReservation;
import com.ayushsingh.doc_helper.features.chat.repository.ChatMessageRepository;
import com.ayushsingh.doc_helper.features.chat.repository.ChatThreadRepository;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;
import com.ayushsingh.doc_helper.features.chat.service.ThreadTurnService;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.ChatContext;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.TokenUsageService;
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

        private final ChatClient.Builder chatClientBuilder;
        private final VectorStore vectorStore;
        private final ChatThreadRepository chatThreadRepository;
        private final ChatMessageRepository chatMessageRepository;
        private final UserDocRepository userDocRepository;
        private final MongoTemplate mongoTemplate;
        private final LoggingAdvisor loggingAdvisor;
        private final WebSearchTool webSearchTool;
        private final TokenUsageService tokenUsageService;
        private final static Long DEFAULT_TOKEN_THRESHOLD = 5000L;
        private final ChatCancellationRegistry chatCancellationRegistry;
        private final ThreadTurnService threadTurnService;

        @Override
        public Flux<String> generateStreamingResponse(ChatRequest chatRequest,
                        Boolean webSearch,
                        String generationId) {
                log.debug("Generating streaming response for documentId: {}, generationId: {}",
                                chatRequest.documentId(), generationId);

                Long userId = UserContext.getCurrentUser().getUser().getId();
                tokenUsageService.checkAndEnforceQuota(userId, DEFAULT_TOKEN_THRESHOLD);

                ChatContext context = prepareChatContext(chatRequest);
                ChatThread thread = context.chatThread();
                String threadId = thread.getId();

                TurnReservation reservation = threadTurnService.reserveTurn(threadId);
                Long turnNumber = reservation.turnNumber();

                saveUserMessage(thread, turnNumber, chatRequest.question());

                StringBuilder fullResponse = new StringBuilder();
                Flux<Void> cancelFlux = chatCancellationRegistry.getOrCreateCancelFlux(generationId);

                return buildChatClientSpec(context, webSearch, generationId)
                                .stream()
                                .content()
                                .takeUntilOther(cancelFlux)
                                .doOnNext(fullResponse::append)
                                .doOnError(error -> log.error(
                                                "Error during streaming response for documentId: {}, generationId: {}",
                                                chatRequest.documentId(), generationId, error))
                                .doFinally(signalType -> handleStream(
                                                signalType,
                                                generationId,
                                                context,
                                                fullResponse,
                                                turnNumber));
        }

        private void handleStream(SignalType signalType,
                        String generationId,
                        ChatContext context,
                        StringBuilder fullResponse,
                        Long turnNumber) {

                boolean userCancelled = chatCancellationRegistry.isManuallyCancelled(generationId);
                chatCancellationRegistry.clear(generationId);

                ChatThread thread = context.chatThread();
                String threadId = thread.getId();

                if (userCancelled) {
                        handleCancel(generationId, context, fullResponse, turnNumber);
                        return;
                }

                switch (signalType) {
                        case ON_COMPLETE -> {
                                handleOnComplete(generationId, context, fullResponse, turnNumber);
                        }
                        case ON_ERROR -> {
                                log.warn("Streaming ERROR for threadId: {}, generationId: {}",
                                                threadId, generationId);
                                // optional: mark turn as failed
                        }
                        case CANCEL -> {
                                handleCancel(generationId, context, fullResponse, turnNumber);
                        }
                        default -> log.debug("Streaming finished with {} for generationId: {}",
                                        signalType, generationId);
                }
        }

        private void handleCancel(
                        String generationId,
                        ChatContext context,
                        StringBuilder fullResponse, Long turnNumber) {

                if (!fullResponse.isEmpty()) {
                        saveAssistantMessage(context.chatThread(), turnNumber, fullResponse.toString());
                }
                log.info("Streaming response CANCELLED for threadId: {}, generationId: {}",
                                context.chatThread().getId(), generationId);
        }

        private void handleOnComplete(
                        String generationId,
                        ChatContext context,
                        StringBuilder fullResponse, Long turnNumber) {
                if (!fullResponse.isEmpty()) {
                        saveAssistantMessage(context.chatThread(), turnNumber, fullResponse.toString());
                        log.info("Streaming response completed for threadId: {}, generationId: {}",
                                        context.chatThread().getId(), generationId);
                } else {
                        log.info("Streaming completed with empty response for generationId: {}",
                                        generationId);
                }
        }

        @Override
        public ChatCallResponse generateResponse(ChatRequest chatRequest,
                        Boolean webSearch) {
                Long userId = UserContext.getCurrentUser().getUser().getId();
                ChatContext context = prepareChatContext(chatRequest);
                String threadId = context.chatThread().getId();
                TurnReservation reservation = threadTurnService.reserveTurn(threadId);
                Long turnNumber = reservation.turnNumber();

                tokenUsageService.checkAndEnforceQuota(userId, DEFAULT_TOKEN_THRESHOLD);

                log.debug("Generating non-streaming response for documentId: {}",
                                chatRequest.documentId());

                saveUserMessage(context.chatThread(), turnNumber, chatRequest.question());

                String responseContent = buildChatClientSpec(context, webSearch, null).call()
                                .content();

                saveAssistantMessage(context.chatThread(), turnNumber, responseContent);

                log.debug("Non-streaming response completed for threadId: {}",
                                threadId);
                return ChatCallResponse.builder().message(responseContent).build();
        }

        private ChatContext prepareChatContext(ChatRequest chatRequest) {
                Long documentId = chatRequest.documentId();
                String userQuestion = chatRequest.question();

                validateDocumentExists(documentId);

                Long userId = UserContext.getCurrentUser().getUser().getId();

                ChatThread chatThread = getOrCreateChatThread(documentId, userId);

                String ragContext = retrieveRagContext(documentId, userId,
                                userQuestion);

                String historyContext = retrieveHistoryContext(chatThread.getId());

                String finalPrompt = PromptTemplates.RAG_PROMPT_TEMPLATE.replace(
                                "{context}",
                                ragContext).replace("{chatHistory}", historyContext);

                SystemMessage systemMessage = new SystemMessage(finalPrompt);
                UserMessage userMessage = new UserMessage(userQuestion);
                Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

                return new ChatContext(documentId, userId, chatThread, prompt,
                                ragContext, historyContext);
        }

        private ChatClient.ChatClientRequestSpec buildChatClientSpec(
                        ChatContext context, Boolean webSearchEnabled, String generationId) {
                ChatClient chatClient = chatClientBuilder.build();

                var chatClientSpec = chatClient.prompt(context.prompt())
                                .advisors(spec -> {
                                        spec.param("documentId", context.documentId())
                                                        .param("userId", context.userId())
                                                        .param("threadId", context.chatThread().getId());
                                        if (generationId != null) {
                                                spec.param("generationId", generationId);
                                        }
                                })
                                .advisors(loggingAdvisor);

                if (webSearchEnabled) {
                        chatClientSpec.tools(webSearchTool);
                }
                return chatClientSpec;
        }

        private void validateDocumentExists(Long documentId) {
                if (!userDocRepository.existsById(documentId)) {
                        log.error("Document not found for documentId: {}", documentId);
                        throw new BaseException(
                                        "Document not found for documentId: " + documentId,
                                        ExceptionCodes.DOCUMENT_NOT_FOUND);
                }
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

        public ChatMessage saveUserMessage(ChatThread thread,
                        Long turnNumber,
                        String content) {
                ChatMessage msg = new ChatMessage();
                msg.setThreadId(thread.getId());
                msg.setTurnNumber(turnNumber);
                msg.setRole(MessageRole.USER);
                msg.setContent(content);
                msg.setTimestamp(Instant.now());

                ChatMessage saved = chatMessageRepository.save(msg);
                updateThreadSnippet(thread.getId(), turnNumber, content);

                return saved;
        }

        public ChatMessage saveAssistantMessage(ChatThread thread,
                        Long turnNumber,
                        String content) {
                ChatMessage msg = new ChatMessage();
                msg.setThreadId(thread.getId());
                msg.setTurnNumber(turnNumber);
                msg.setRole(MessageRole.ASSISTANT);
                msg.setContent(content);
                msg.setTimestamp(Instant.now());

                ChatMessage saved = chatMessageRepository.save(msg);

                updateThreadSnippet(thread.getId(), turnNumber, content);

                return saved;
        }

        private void updateThreadSnippet(String threadId,
                        Long turnNumber,
                        String content) {

                String snippet = computeSnippet(content);

                Query query = new Query(
                                Criteria.where("_id").is(threadId)
                                                .orOperator(
                                                                Criteria.where("lastSnippetTurnNumber").lt(turnNumber),
                                                                Criteria.where("lastSnippetTurnNumber").exists(false)));

                Update update = new Update()
                                .set("lastMessageSnippet", snippet)
                                .set("lastSnippetTurnNumber", turnNumber)
                                .set("updatedAt", Instant.now());

                mongoTemplate.updateFirst(query, update, ChatThread.class);
        }

        private String computeSnippet(String content) {
                if (content == null) {
                        return null;
                }
                int maxLen = 50;
                return content.length() > maxLen
                                ? content.substring(0, maxLen) + "..."
                                : content;
        }

        @Override
        public ChatHistoryResponse fetchChatHistoryForDocument(Long documentId,
                        Integer page) {
                Long userId = UserContext.getCurrentUser().getUser().getId();
                var chatThread = chatThreadRepository.findByDocumentIdAndUserId(
                                documentId, userId);
                if (chatThread.isPresent()) {
                        PageRequest pageRequest = PageRequest.of(
                                        page,
                                        10,
                                        Sort.by(Sort.Direction.DESC, "turnNumber")
                                                        .and(Sort.by(Sort.Direction.DESC, "timestamp")));
                        List<ChatMessage> recentMessages = chatMessageRepository.findByThreadId(
                                        chatThread.get().getId(), pageRequest);

                        List<ChatMessageResponse> messageResponses = recentMessages.stream()
                                        .map(msg -> new ChatMessageResponse(msg.getId(),
                                                        msg.getContent(), msg.getRole(),
                                                        msg.getTimestamp()))
                                        .collect(Collectors.toList());

                        String threadId = recentMessages.isEmpty() ? null : recentMessages.getFirst().getThreadId();

                        return new ChatHistoryResponse(threadId, messageResponses);
                } else {
                        return new ChatHistoryResponse();
                }
        }

        @Override
        public Boolean deleteChatHistoryForDocument(Long documentId) {
                var query = new Query();
                var userId = UserContext.getCurrentUser().getUser().getId();
                var chatThread = chatThreadRepository.findByDocumentIdAndUserId(
                                documentId, userId);
                query.addCriteria(Criteria.where("documentId")
                                .is(documentId)
                                .and("userId")
                                .is(userId));

                var result = mongoTemplate.remove(query, ChatThread.class);

                if (result.getDeletedCount() > 0) {
                        if (chatThread.isPresent()) {
                                var threadQuery = new Query();
                                threadQuery.addCriteria(Criteria.where("threadId")
                                                .is(chatThread.get().getId()));
                                mongoTemplate.remove(threadQuery, ChatMessage.class);
                        }
                }

                return true;
        }

        private ChatThread getOrCreateChatThread(Long documentId, Long userId) {
                log.debug("Getting or creating chat thread for documentId: {}",
                                documentId);
                Query query = new Query(Criteria.where("documentId")
                                .is(documentId)
                                .and("userId")
                                .is(userId));
                Update update = new Update().setOnInsert("documentId", documentId)
                                .setOnInsert("userId", userId)
                                .setOnInsert("createdAt", Instant.now())
                                .set("updatedAt", Instant.now());

                mongoTemplate.upsert(query, update, ChatThread.class);
                return mongoTemplate.findOne(query, ChatThread.class);
        }
}
