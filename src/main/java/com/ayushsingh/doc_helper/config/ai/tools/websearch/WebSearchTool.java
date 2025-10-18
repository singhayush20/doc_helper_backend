package com.ayushsingh.doc_helper.config.ai.tools.websearch;

import com.ayushsingh.doc_helper.config.ai.tools.websearch.dto.WebSearchRequest;
import com.ayushsingh.doc_helper.config.ai.tools.websearch.dto.WebSearchResult;
import com.ayushsingh.doc_helper.config.ai.tools.websearch.searchprovider.WebSearchProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
@Slf4j
public class WebSearchTool {

    private final WebSearchProvider provider;

    @Tool(name = "web_search", description = """
            Search the public web for recent or external information not present in the document.
            Use for current events, releases, figures, or facts requiring freshness; return concise, citable results.
            """)
    public WebSearchResult search(@Valid WebSearchRequest request) {
        String q = request.getQuery() == null ? "" : request.getQuery().strip();
        if (q.length() < 3) {
            return WebSearchResult.failure("Query too short", q);
        }
        try {
            WebSearchResult out = provider.search(request);
            int maxResults =
                    request.getMaxResults() != null ? request.getMaxResults() :
                            3;
            int maxChars = request.getMaxSnippetChars() != null ?
                    request.getMaxSnippetChars() : 600;
            return out.truncatedTo(maxResults, maxChars);
        } catch (Exception e) {
            log.warn("Web search tool failed: {}", e.getMessage());
            return WebSearchResult.failure(
                    "Web search error: " + e.getMessage(), q);
        }
    }
}
