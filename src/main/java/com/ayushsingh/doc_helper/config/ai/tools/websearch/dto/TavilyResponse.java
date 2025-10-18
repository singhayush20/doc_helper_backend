package com.ayushsingh.doc_helper.config.ai.tools.websearch.dto;

import java.util.List;

public record TavilyResponse(
        String query,
        String follow_up_questions,
        String answer,
        List<String> images,
        List<TavilyItem> results,
        Double response_time,
        String request_id
) {}
