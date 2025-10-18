package com.ayushsingh.doc_helper.config.ai.tools.websearch.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class WebSearchResult {
    boolean success;
    String message;
    String query;
    String answer;       // Tavily synthesized answer if include_answer=true
    String requestId;    // Tavily request_id for traceability
    Instant retrievedAt;
    List<WebSearchItem> results;

    public static WebSearchResult failure(String msg, String query) {
        return WebSearchResult.builder()
                .success(false)
                .message(msg)
                .query(query)
                .answer("")
                .requestId("")
                .retrievedAt(Instant.now())
                .results(List.of())
                .build();
    }

    public WebSearchResult truncatedTo(int maxResults, int maxChars) {
        if (results == null || results.isEmpty()) return this;
        List<WebSearchItem> clipped = results.stream()
                .limit(Math.max(1, Math.min(8, maxResults)))
                .map(i -> i.truncate(maxChars))
                .toList();
        return WebSearchResult.builder()
                .success(success)
                .message(message)
                .query(query)
                .answer(answer)
                .requestId(requestId)
                .retrievedAt(retrievedAt)
                .results(clipped)
                .build();
    }
}
