package com.ayushsingh.doc_helper.config.ai.tools.websearch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class WebSearchRequest {
    @NotBlank
    @Size(min = 3, max = 300)
    String query;

    @Min(1) @Max(8)
    Integer maxResults;

    @Min(80) @Max(1500)
    Integer maxSnippetChars;

    // Recency bias; provider maps 'days' to its time filter
    @Min(1) @Max(3650)
    Integer daysBack;

    // Optional host filters; provider may honor as include/exclude_domains
    List<@NotBlank String> siteAllowList;
    List<@NotBlank String> siteDenyList;
}
