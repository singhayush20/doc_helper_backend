package com.ayushsingh.doc_helper.core.ai.tools.websearch.dto;

public record WebSearchItem(
        String title,
        String url, // canonical stable URL (used for citations)
        String snippet, // plain text, clipped
        String source, // host/domain
        String publishedAt, // ISO date string if available
        Double score // relevance if provided
) {

    WebSearchItem truncate(int maxChars) {
        String s = snippet == null ? "" : snippet;
        if (s.length() <= maxChars)
            return this;
        return WebSearchItem.builder()
                .title(title)
                .url(url)
                .snippet(s.substring(0, Math.max(0, maxChars - 1)) + "…")
                .source(source)
                .publishedAt(publishedAt)
                .score(score)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String url;
        private String snippet;
        private String source;
        private String publishedAt;
        private Double score;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder snippet(String snippet) {
            this.snippet = snippet;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder publishedAt(String publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder score(Double score) {
            this.score = score;
            return this;
        }

        public WebSearchItem build() {
            return new WebSearchItem(title, url, snippet, source, publishedAt, score);
        }
    }
}
