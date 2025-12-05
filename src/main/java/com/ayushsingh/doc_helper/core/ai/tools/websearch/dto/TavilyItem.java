package com.ayushsingh.doc_helper.core.ai.tools.websearch.dto;

public record TavilyItem(
        String url,
        String title,
        String content,
        Double score,
        String raw_content,
        String published_date
) {}
