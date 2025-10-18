package com.ayushsingh.doc_helper.config.ai.tools.websearch.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WebSearchItem {
    String title;
    String url;          // canonical stable URL (used for citations)
    String snippet;      // plain text, clipped
    String source;       // host/domain
    String publishedAt;  // ISO date string if available
    Double score;        // relevance if provided

    WebSearchItem truncate(int maxChars) {
        String s = snippet == null ? "" : snippet;
        if (s.length() <= maxChars) return this;
        return WebSearchItem.builder()
                .title(title)
                .url(url)
                .snippet(s.substring(0, Math.max(0, maxChars - 1)) + "…")
                .source(source)
                .publishedAt(publishedAt)
                .score(score)
                .build();
    }
}
