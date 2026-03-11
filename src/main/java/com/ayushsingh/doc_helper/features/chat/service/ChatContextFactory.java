package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.entity.ChatThread;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.ChatContext;
import com.ayushsingh.doc_helper.features.user_doc.repository.UserDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatContextFactory {

    private static final int MAX_RAG_CHARS = 2500;

    private final VectorStore vectorStore;
    private final UserDocRepository userDocRepository;
    private final ChatPersistenceService chatPersistenceService;
    private final ChatPromptBuilder chatPromptBuilder;

    public ChatContext prepare(ChatRequest chatRequest, boolean webSearchEnabled) {
        Long documentId = chatRequest.documentId();
        String userQuestion = chatRequest.question();
        validateDocumentExists(documentId);

        Long userId = UserContext.getCurrentUser().getUser().getId();
        ChatThread chatThread = chatPersistenceService.getOrCreateChatThread(documentId, userId);

        List<Document> ragDocuments = retrieveRagDocuments(documentId, userId, userQuestion);
        String ragContext = buildRagContext(ragDocuments);
        String historyContext = chatPersistenceService.buildHistoryContext(chatThread);
        Prompt prompt = chatPromptBuilder.build(
                historyContext,
                userQuestion,
                ragDocuments,
                webSearchEnabled
        );

        return new ChatContext(
                documentId,
                userId,
                chatThread,
                prompt,
                ragContext,
                historyContext,
                ragDocuments
        );
    }

    private void validateDocumentExists(Long documentId) {
        if (!userDocRepository.existsById(documentId)) {
            log.error("Document not found for documentId: {}", documentId);
            throw new BaseException(
                    "Document not found for documentId: " + documentId,
                    ExceptionCodes.DOCUMENT_NOT_FOUND
            );
        }
    }

    private List<Document> retrieveRagDocuments(Long documentId,
                                                Long userId,
                                                String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(12)
                .filterExpression("userId == " + userId + " && documentId == " + documentId)
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);
        return documents.stream()
                .filter(document -> {
                    Float distance = (Float) document.getMetadata().get("distance");
                    return distance == null || distance < 0.45f;
                })
                .toList();
    }

    private String buildRagContext(List<Document> documents) {
        StringBuilder context = new StringBuilder();
        int usedCharacters = 0;

        for (Document document : documents) {
            String text = document.getFormattedContent();
            if (usedCharacters + text.length() > MAX_RAG_CHARS) {
                break;
            }
            context.append(text).append("\n---\n");
            usedCharacters += text.length();
        }
        return context.toString();
    }
}
