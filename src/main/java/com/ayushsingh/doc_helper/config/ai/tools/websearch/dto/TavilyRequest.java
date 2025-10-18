package com.ayushsingh.doc_helper.config.ai.tools.websearch.dto;

import java.util.List;

public record TavilyRequest(
        String query,
        Boolean auto_parameters,
        String topic,               // "general" | "news" | "finance"
        String search_depth,        // "basic" | "advanced"
        Integer chunks_per_source,
        Integer max_results,
        String time_range,          // "day" | "week" | "month" | "year" | null
        Integer days,
        Boolean include_answer,
        Boolean include_raw_content,
        Boolean include_images,
        Boolean include_image_descriptions,
        Boolean include_favicon,
        List<String> include_domains,
        List<String> exclude_domains,
        String country
) {}
