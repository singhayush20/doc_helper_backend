package com.ayushsingh.doc_helper.features.chat.service.service_impl;

import com.ayushsingh.doc_helper.core.ai.ChatCancellationRegistry;
import com.ayushsingh.doc_helper.core.ai.advisors.LoggingAdvisor;
import com.ayushsingh.doc_helper.core.ai.advisors.ToolCallAdvisor;
import com.ayushsingh.doc_helper.core.ai.tools.websearch.WebSearchTool;
import com.ayushsingh.doc_helper.core.ai.tools.websearch.dto.WebSearchItem;
import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatHistoryResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatMessageResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.entity.*;
import com.ayushsingh.doc_helper.features.chat.repository.ChatMessageRepository;
import com.ayushsingh.doc_helper.features.chat.repository.ChatThreadRepository;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;
import com.ayushsingh.doc_helper.features.chat.service.ChatSummaryService;
import com.ayushsingh.doc_helper.features.chat.service.CitationBuilder;
import com.ayushsingh.doc_helper.features.chat.service.ThreadTurnService;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.ChatContext;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import com.ayushsingh.doc_helper.features.user_activity.dto.ActivityTarget;
import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityType;
import com.ayushsingh.doc_helper.features.user_activity.service.UserActivityRecorder;
import com.ayushsingh.doc_helper.features.user_doc.repository.UserDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final VectorStore vectorStore;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserDocRepository userDocRepository;
    private final MongoTemplate mongoTemplate;
    private final LoggingAdvisor loggingAdvisor;
    private final WebSearchTool webSearchTool;
    private final QuotaManagementService quotaManagementService;
    private final ChatCancellationRegistry chatCancellationRegistry;
    private final ThreadTurnService threadTurnService;
    private final ChatSummaryService chatSummaryService;
    private final UserActivityRecorder userActivityRecorder;
    private final ChatClient chatClient;
    private final ToolCallAdvisor toolCallAdvisor;
    private final CitationBuilder citationBuilder;

    @Value("${doc-chat.model}")
    String modelName;
    @Value("${doc-chat.temperature}")
    Double temperature;

    private final static Long DEFAULT_TOKEN_THRESHOLD = 6800L;
    private static final int MAX_RAG_CHARS = 2500;

    @Override
    public Flux<ServerSentEvent<ChatCallResponse>> generateStreamingResponse(ChatRequest chatRequest,
                                                                             Boolean webSearch,
                                                                             String generationId) {
        log.debug("Generating streaming response for documentId: {}, generationId: {}",
                chatRequest.documentId(), generationId);

        Long userId = UserContext.getCurrentUser().getUser().getId();
        quotaManagementService.checkAndEnforceQuota(userId, DEFAULT_TOKEN_THRESHOLD);

        ChatContext context = prepareChatContext(chatRequest);
        ChatThread thread = context.chatThread();
        String threadId = thread.getId();

        TurnReservation reservation = threadTurnService.reserveTurn(threadId);
        Long turnNumber = reservation.turnNumber();

        saveUserMessage(thread, turnNumber, chatRequest.question());

        StringBuilder fullResponse = new StringBuilder();
        Flux<Void> cancelFlux = chatCancellationRegistry.getOrCreateCancelFlux(generationId);
        AtomicReference<ChatClientResponse> lastClientResponse = new AtomicReference<>();

        Flux<ServerSentEvent<ChatCallResponse>> tokenFlux = buildChatClientSpec(context,
                webSearch, generationId)
                .stream()
                .chatClientResponse()
                .takeUntilOther(cancelFlux)
                .doOnNext(clientResponse -> {
                    lastClientResponse.set(clientResponse);
                    var chatResponse = clientResponse.chatResponse();
                    if (chatResponse != null) {
                        String token = chatResponse.getResult().getOutput().getText();
                        if (token != null) fullResponse.append(token);
                    }
                })
                .map(clientResponse -> {
                    var chatResponse = clientResponse.chatResponse();
                    if (chatResponse != null) {
                        String token = chatResponse
                                .getResult().getOutput().getText();
                        return ServerSentEvent.<ChatCallResponse>builder()
                                .event("token")
                                .data(new ChatCallResponse(token != null ? token
                                        : "", null, null, null))
                                .build();
                    } else {
                        return ServerSentEvent.<ChatCallResponse>builder().event("token").build();
                    }
                })
                .doOnError(error -> log.error(
                        "Error during streaming response for documentId: {}, generationId: {}",
                        chatRequest.documentId(), generationId, error))
                .doFinally(signalType -> handleStream(
                        signalType,
                        generationId,
                        context,
                        fullResponse,
                        turnNumber,
                        lastClientResponse,
                        webSearch));

        Flux<ServerSentEvent<ChatCallResponse>> citationsFlux = Mono
                .fromCallable(() -> {
                    @SuppressWarnings("unchecked")
                    List<WebSearchItem> webItems = webSearch
                            ? Optional.ofNullable(lastClientResponse.get())
                            .map(r -> (List<WebSearchItem>) r.context()
                                    .getOrDefault(ToolCallAdvisor.WEB_CITATIONS_KEY, List.of()))
                            .orElse(List.of())
                            : List.of();
                    return citationBuilder.build(context.ragDocuments(), webItems);
                })
                .map(citations -> {
                    return ServerSentEvent.<ChatCallResponse>builder()
                            .event("citations")
                            .data(new ChatCallResponse(null, citations,
                                    null, null))
                            .build();
                })
                .flux();

        return tokenFlux.concatWith(citationsFlux);
    }

    private void handleStream(SignalType signalType,
                              String generationId,
                              ChatContext context,
                              StringBuilder fullResponse,
                              Long turnNumber,
                              AtomicReference<ChatClientResponse> lastClientResponse,
                              Boolean webSearch) {

        boolean userCancelled = chatCancellationRegistry.isManuallyCancelled(generationId);
        chatCancellationRegistry.clear(generationId);

        ChatThread thread = context.chatThread();
        String threadId = thread.getId();

        if (userCancelled) {
            handleCancel(generationId, context, fullResponse, turnNumber);
            return;
        }

        switch (signalType) {
            case ON_COMPLETE -> handleOnComplete(
                    generationId, context, fullResponse, turnNumber,
                    lastClientResponse, webSearch);
            case ON_ERROR ->
                    log.warn("Streaming ERROR for threadId: {}, generationId: {}",
                            threadId, generationId);
            case CANCEL ->
                    handleCancel(generationId, context, fullResponse, turnNumber);
            default ->
                    log.debug("Streaming finished with {} for generationId: {}",
                            signalType, generationId);
        }
    }

    private void handleCancel(
            String generationId,
            ChatContext context,
            StringBuilder fullResponse,
            Long turnNumber) {

        if (!fullResponse.isEmpty()) {
            saveAssistantMessage(context.chatThread(), turnNumber,
                    fullResponse.toString(), null);
            if (turnNumber % 6 == 0) {
                chatSummaryService.summarizeThread(context.chatThread().getId());
            }
        }
        log.info("Streaming response CANCELLED for threadId: {}, generationId: {}",
                context.chatThread().getId(), generationId);
    }

    private void handleOnComplete(
            String generationId,
            ChatContext context,
            StringBuilder fullResponse,
            Long turnNumber,
            AtomicReference<ChatClientResponse> lastClientResponse,
            Boolean webSearch) {

        if (!fullResponse.isEmpty()) {
            // Extract web citations from the advisor context on the last response chunk.
            // ToolCallAdvisor uses ChatClientMessageAggregator so citations are available
            // on the final aggregated chunk, not on every intermediate token.
            @SuppressWarnings("unchecked")
            List<WebSearchItem> webItems = webSearch
                    ? Optional.ofNullable(lastClientResponse.get())
                    .map(r -> (List<WebSearchItem>) r.context()
                            .getOrDefault(ToolCallAdvisor.WEB_CITATIONS_KEY, List.of()))
                    .orElse(List.of())
                    : List.of();

            List<ChatResponseCitation> citations =
                    citationBuilder.build(context.ragDocuments(), webItems);

            log.debug("Stream citations for threadId {}: {}",
                    context.chatThread().getId(), citations);

            saveAssistantMessage(context.chatThread(), turnNumber,
                    fullResponse.toString(), citations);

            log.info("Streaming response completed for threadId: {}, generationId: {}",
                    context.chatThread().getId(), generationId);
        } else {
            log.info("Streaming completed with empty response for generationId: {}",
                    generationId);
        }

        if (turnNumber % 6 == 0) {
            chatSummaryService.summarizeThread(context.chatThread().getId());
        }

        userActivityRecorder.record(
                context.userId(),
                new ActivityTarget(ActivityTargetType.USER_DOC, context.documentId()),
                UserActivityType.DOCUMENT_CHAT);
    }

    @Override
    public ChatCallResponse generateResponse(ChatRequest chatRequest,
                                             Boolean webSearch) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        ChatContext context = prepareChatContext(chatRequest);
        String threadId = context.chatThread().getId();
        TurnReservation reservation = threadTurnService.reserveTurn(threadId);
        Long turnNumber = reservation.turnNumber();

        quotaManagementService.checkAndEnforceQuota(userId, DEFAULT_TOKEN_THRESHOLD);

        log.debug("Generating non-streaming response for documentId: {}",
                chatRequest.documentId());

        saveUserMessage(context.chatThread(), turnNumber, chatRequest.question());

        ChatClientResponse clientResponse = buildChatClientSpec(context, webSearch,
                null).call().chatClientResponse();
        ChatResponse chatResponse = clientResponse.chatResponse();

        if (chatResponse != null) {
            String responseContent = chatResponse.getResult().getOutput().getText();

            @SuppressWarnings("unchecked")
            List<WebSearchItem> webItems = webSearch
                    ? (List<WebSearchItem>) clientResponse.context()
                    .getOrDefault(ToolCallAdvisor.WEB_CITATIONS_KEY, List.of())
                    : List.of();

            List<ChatResponseCitation> citations =
                    citationBuilder.build(context.ragDocuments(),
                            webItems);

            saveAssistantMessage(context.chatThread(), turnNumber,
                    responseContent, citations);

            if (turnNumber % 6 == 0) {
                chatSummaryService.summarizeThread(threadId);
            }

            userActivityRecorder.record(context.userId(), new ActivityTarget(ActivityTargetType.USER_DOC, context.documentId()), UserActivityType.DOCUMENT_CHAT);

            log.debug("Non-streaming response completed for threadId: {}",
                    threadId);
            return ChatCallResponse.builder()
                    .message(responseContent)
                    .citations(citations)
                    .build();
        } else {
            return new ChatCallResponse();
        }
    }

    private ChatClient.ChatClientRequestSpec buildChatClientSpec(
            ChatContext context, Boolean webSearchEnabled, String generationId) {

        var chatClientSpec = chatClient.prompt(context.prompt())
                .options(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(temperature)
                        .build())
                .advisors(spec -> {
                    spec.param("documentId", context.documentId())
                            .param("userId", context.userId())
                            .param("threadId", context.chatThread().getId());
                    if (generationId != null) {
                        spec.param("generationId", generationId);
                    }
                })
                .advisors(loggingAdvisor)
                .advisors(toolCallAdvisor);

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

    // Returns raw docs (kept for citation building)
    private List<Document> retrieveRagDocuments(Long documentId, Long userId, String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(12)
                .filterExpression("userId == " + userId + " && documentId == " + documentId)
                .build();

        List<Document> docs = vectorStore.similaritySearch(request);

        return docs.stream()
                .filter(doc -> {
                    Float dist = (Float) doc.getMetadata().get("distance");
                    return dist == null || dist < 0.45f;
                })
                .toList();
    }

    private ChatContext prepareChatContext(ChatRequest chatRequest) {
        Long documentId = chatRequest.documentId();
        String userQuestion = chatRequest.question();
        validateDocumentExists(documentId);

        Long userId = UserContext.getCurrentUser().getUser().getId();
        ChatThread chatThread = getOrCreateChatThread(documentId, userId);

        List<Document> ragDocuments = retrieveRagDocuments(documentId, userId, userQuestion);
        String ragContext = buildRagContext(ragDocuments);   // existing method, unchanged
        String historyContext = retrieveHistoryContext(chatThread);

        Prompt prompt = getUserPrompt(ragContext, historyContext, userQuestion, ragDocuments);

        return new ChatContext(documentId, userId, chatThread, prompt,
                ragContext, historyContext, ragDocuments);
    }

    private static Prompt getUserPrompt(String ragContext,
                                        String historyContext,
                                        String userQuestion,
                                        List<Document> ragDocuments) {

        // Number each chunk so the model can emit [1], [2] inline citations
        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < ragDocuments.size(); i++) {
            numbered.append("[").append(i + 1).append("] ")
                    .append(ragDocuments.get(i).getFormattedContent())
                    .append("\n---\n");
        }

        String userPrompt = """
                ## Document Sources
                %s
                
                ## Conversation History
                %s
                
                ## Question
                %s
                
                """.formatted(numbered, historyContext, userQuestion);

        SystemMessage systemMessage = new SystemMessage(
                "You are an assistant that answers using the provided context. " +
                        "If context is insufficient, you can use the " +
                        "web_search tool to find answers from the web. If the" +
                        " tool is not available, just say that you don't know" +
                        "." +
                        ".");

        return new Prompt(List.of(systemMessage, new UserMessage(userPrompt)));
    }

    private String buildRagContext(List<Document> docs) {

        StringBuilder sb = new StringBuilder();
        int used = 0;

        for (Document d : docs) {
            String text = d.getFormattedContent();

            if (used + text.length() > MAX_RAG_CHARS)
                break;

            sb.append(text).append("\n---\n");
            used += text.length();
        }

        return sb.toString();
    }

    private String retrieveHistoryContext(ChatThread thread) {

        String summary = chatSummaryService.getCachedSummary(thread);

        List<ChatMessage> lastTurns = chatMessageRepository.findLastNTurns(thread.getId(), 4);

        String shortHistory = lastTurns.stream()
                .map(m -> m.getRole().getValue() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder();

        if (summary != null && !summary.isBlank()) {
            sb.append("Conversation summary:\n")
                    .append(summary)
                    .append("\n\n");
        }

        sb.append("Recent messages:\n").append(shortHistory);

        return sb.toString();
    }

    public void saveUserMessage(ChatThread thread,
                                Long turnNumber,
                                String content) {
        ChatMessage msg = new ChatMessage();
        msg.setThreadId(thread.getId());
        msg.setTurnNumber(turnNumber);
        msg.setRole(MessageRole.USER);
        msg.setContent(content);
        msg.setTimestamp(Instant.now());

        chatMessageRepository.save(msg);
        updateThreadSnippet(thread.getId(), turnNumber, content);

    }

    public void saveAssistantMessage(ChatThread thread,
                                     Long turnNumber,
                                     String content,
                                     List<ChatResponseCitation> citations) {
        ChatMessage msg = new ChatMessage();
        msg.setThreadId(thread.getId());
        msg.setTurnNumber(turnNumber);
        msg.setRole(MessageRole.ASSISTANT);
        msg.setContent(content);
        msg.setCitations(citations);
        msg.setTimestamp(Instant.now());

        chatMessageRepository.save(msg);

        updateThreadSnippet(thread.getId(), turnNumber, content);

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
