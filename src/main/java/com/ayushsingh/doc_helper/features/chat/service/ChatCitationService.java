package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.core.ai.tools.websearch.dto.WebSearchItem;
import com.ayushsingh.doc_helper.features.chat.entity.ChatResponseCitation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCitationService {

    private final CitationBuilder citationBuilder;

    public List<ChatResponseCitation> build(List<Document> ragDocuments,
                                            boolean webSearchRequested) {
        return citationBuilder.build(
                ragDocuments,
                extractWebItems(webSearchRequested)
        );
    }

    @SuppressWarnings("unchecked")
    public List<WebSearchItem> extractWebItems(boolean webSearchRequested) {
        if(webSearchRequested) {
            // TODO: Implement this
            return List.of();
        }
        else {
            return List.of();
        }
    }
}
