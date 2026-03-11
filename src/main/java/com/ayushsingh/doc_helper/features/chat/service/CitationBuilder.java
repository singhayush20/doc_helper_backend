package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.core.ai.tools.websearch.dto.WebSearchItem;
import com.ayushsingh.doc_helper.features.chat.entity.ChatResponseCitation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CitationBuilder {

    private static final int SNIPPET_MAX_LEN = 200;

    public List<ChatResponseCitation> build(List<Document> ragDocs,
                                            List<WebSearchItem> webItems) {
        List<ChatResponseCitation> citations = new ArrayList<>();
        int index = 1;

        for (Document doc : ragDocs) {
            Map<String, Object> meta = doc.getMetadata();

            // Try common metadata keys your ingestion pipeline might use
            String title = firstNonNull(
                    (String) meta.get("source"),
                    (String) meta.get("file_name"),
                    (String) meta.get("fileName"),
                    "Document"
            );
            Object page = meta.get("page_number") != null
                    ? meta.get("page_number") : meta.get("page");
            Float dist = (Float) meta.get("distance");

            citations.add(new ChatResponseCitation(
                    index++,
                    ChatResponseCitation.CitationType.DOCUMENT,
                    title,
                    null,
                    truncate(doc.getText(), SNIPPET_MAX_LEN),
                    page,
                    dist != null ? dist.doubleValue() : null
            ));
        }

        for (WebSearchItem item : webItems) {
            citations.add(new ChatResponseCitation(
                    index++,
                    ChatResponseCitation.CitationType.WEB,
                    item.title(),
                    item.url(),
                    truncate(item.snippet(), SNIPPET_MAX_LEN),
                    null,
                    null
            ));
        }

        log.debug("Generated citations: {}",citations);

        return citations;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "…" : text;
    }

    private String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "Unknown";
    }
}