package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.core.ai.advisors.ToolCallAdvisor;
import com.ayushsingh.doc_helper.core.ai.tools.websearch.dto.WebSearchItem;
import com.ayushsingh.doc_helper.features.chat.entity.ChatResponseCitation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCitationService {

    private final CitationBuilder citationBuilder;

    public List<ChatResponseCitation> build(List<Document> ragDocuments,
                                            ChatClientResponse clientResponse,
                                            boolean webSearchRequested) {
        return citationBuilder.build(
                ragDocuments,
                extractWebItems(clientResponse, webSearchRequested)
        );
    }

    @SuppressWarnings("unchecked")
    public List<WebSearchItem> extractWebItems(ChatClientResponse clientResponse,
                                               boolean webSearchRequested) {
        if (!webSearchRequested || clientResponse == null) {
            return List.of();
        }

        Object webItems = clientResponse.context()
                .getOrDefault(ToolCallAdvisor.WEB_CITATIONS_KEY, List.of());
        log.debug("Extracted web search items: {}",
                webItems);

        if (webItems instanceof List<?> items) {
            return (List<WebSearchItem>) items;
        }
        return List.of();
    }
}
