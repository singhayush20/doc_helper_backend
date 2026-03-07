package com.ayushsingh.doc_helper.features.chat.entity;

public record ChatResponseCitation(
        int index,
        CitationType type,
        String title,    // filename for docs, page title for web
        String url,      // null for document chunks
        String snippet,  // short preview of matched content
        Object page,     // page number if available in metadata
        Double score     // similarity distance score for vector docs
) {
    public enum CitationType { DOCUMENT, WEB }
}