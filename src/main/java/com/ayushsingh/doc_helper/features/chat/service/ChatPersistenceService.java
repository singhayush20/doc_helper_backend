package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.features.chat.dto.ChatHistoryResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatMessageResponse;
import com.ayushsingh.doc_helper.features.chat.entity.ChatMessage;
import com.ayushsingh.doc_helper.features.chat.entity.ChatResponseCitation;
import com.ayushsingh.doc_helper.features.chat.entity.ChatThread;
import com.ayushsingh.doc_helper.features.chat.entity.MessageRole;
import com.ayushsingh.doc_helper.features.chat.repository.ChatMessageRepository;
import com.ayushsingh.doc_helper.features.chat.repository.ChatThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPersistenceService {

    private static final int SNIPPET_MAX_LENGTH = 50;

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MongoTemplate mongoTemplate;
    private final ChatSummaryService chatSummaryService;

    public ChatThread getOrCreateChatThread(Long documentId, Long userId) {
        log.debug("Getting or creating chat thread for documentId: {}", documentId);
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

    public void saveUserMessage(ChatThread thread,
                                Long turnNumber,
                                String content) {
        ChatMessage userMessage = new ChatMessage();
        userMessage.setThreadId(thread.getId());
        userMessage.setTurnNumber(turnNumber);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(content);
        userMessage.setTimestamp(Instant.now());

        chatMessageRepository.save(userMessage);
        updateThreadSnippet(thread.getId(), turnNumber, content);
    }

    public void saveAssistantMessage(ChatThread thread,
                                     Long turnNumber,
                                     String content,
                                     List<ChatResponseCitation> citations) {
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setThreadId(thread.getId());
        assistantMessage.setTurnNumber(turnNumber);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(content);
        assistantMessage.setCitations(citations);
        assistantMessage.setTimestamp(Instant.now());

        chatMessageRepository.save(assistantMessage);
        updateThreadSnippet(thread.getId(), turnNumber, content);
    }

    public String buildHistoryContext(ChatThread thread) {
        String summary = chatSummaryService.getCachedSummary(thread);
        List<ChatMessage> lastTurns = chatMessageRepository.findLastNTurns(thread.getId(), 4);

        String shortHistory = lastTurns.stream()
                .map(message -> message.getRole().getValue() + ": " + message.getContent())
                .collect(Collectors.joining("\n"));

        StringBuilder context = new StringBuilder();
        if (summary != null && !summary.isBlank()) {
            context.append("Conversation summary:\n")
                    .append(summary)
                    .append("\n\n");
        }
        context.append("Recent messages:\n")
                .append(shortHistory);
        return context.toString();
    }

    public ChatHistoryResponse fetchChatHistoryForDocument(Long documentId,
                                                           Long userId,
                                                           Integer page) {
        var chatThread = chatThreadRepository.findByDocumentIdAndUserId(documentId, userId);
        if (chatThread.isEmpty()) {
            return new ChatHistoryResponse();
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                10,
                Sort.by(Sort.Direction.DESC, "turnNumber")
                        .and(Sort.by(Sort.Direction.DESC, "timestamp"))
        );

        List<ChatMessage> recentMessages = chatMessageRepository.findByThreadId(
                chatThread.get().getId(), pageRequest);

        List<ChatMessageResponse> messageResponses = recentMessages.stream()
                .map(message -> ChatMessageResponse.builder()
                        .id(message.getId())
                        .content(message.getContent())
                        .role(message.getRole())
                        .timestamp(message.getTimestamp())
                        .citations(message.getCitations())
                        .build())
                .toList();

        String threadId = recentMessages.isEmpty()
                ? null
                : recentMessages.getFirst().getThreadId();

        return new ChatHistoryResponse(threadId, messageResponses);
    }

    public Boolean deleteChatHistoryForDocument(Long documentId, Long userId) {
        Query query = new Query();
        var chatThread = chatThreadRepository.findByDocumentIdAndUserId(documentId, userId);
        query.addCriteria(Criteria.where("documentId")
                .is(documentId)
                .and("userId")
                .is(userId));

        var result = mongoTemplate.remove(query, ChatThread.class);
        if (result.getDeletedCount() > 0 && chatThread.isPresent()) {
            Query threadQuery = new Query();
            threadQuery.addCriteria(Criteria.where("threadId").is(chatThread.get().getId()));
            mongoTemplate.remove(threadQuery, ChatMessage.class);
        }

        return true;
    }

    private void updateThreadSnippet(String threadId,
                                     Long turnNumber,
                                     String content) {
        String snippet = computeSnippet(content);

        Query query = new Query(
                Criteria.where("_id").is(threadId)
                        .orOperator(
                                Criteria.where("lastSnippetTurnNumber").lt(turnNumber),
                                Criteria.where("lastSnippetTurnNumber").exists(false)
                        )
        );

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
        return content.length() > SNIPPET_MAX_LENGTH
                ? content.substring(0, SNIPPET_MAX_LENGTH) + "..."
                : content;
    }
}
