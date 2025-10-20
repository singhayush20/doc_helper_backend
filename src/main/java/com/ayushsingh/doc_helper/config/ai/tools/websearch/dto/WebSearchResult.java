package com.ayushsingh.doc_helper.config.ai.tools.websearch.dto;

import java.time.Instant;
import java.util.List;

public record WebSearchResult(
        boolean success,
        String message,
        String query,
        String answer,
        String requestId,
        Instant retrievedAt,
        List<WebSearchItem> results) {
    public static WebSearchResult failure(String message, String query) {
        return new WebSearchResult(
                false,
                message,
                query,
                "",
                "",
                Instant.now(),
                List.of());
    }

    public WebSearchResult truncatedTo(int maxResults, int maxChars) {
        if (results == null || results.isEmpty())
            return this;

        List<WebSearchItem> clipped = results.stream()
                .limit(Math.max(1, Math.min(8, maxResults)))
                .map(i -> i.truncate(maxChars))
                .toList();

        return new WebSearchResult(
                success, message, query, answer, requestId, retrievedAt, clipped);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private String message;
        private String query;
        private String answer;
        private String requestId;
        private Instant retrievedAt;
        private List<WebSearchItem> results;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder answer(String answer) {
            this.answer = answer;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder retrievedAt(Instant retrievedAt) {
            this.retrievedAt = retrievedAt;
            return this;
        }

        public Builder results(List<WebSearchItem> results) {
            this.results = results;
            return this;
        }

        public WebSearchResult build() {
            return new WebSearchResult(success, message, query, answer, requestId, retrievedAt, results);
        }
    }
}