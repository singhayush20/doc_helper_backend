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
    private final WebSearchConfig webSearchConfig;

    @Tool(name = "web_search", description = """
            Search the public web for recent or external information not present in the document.
            Use for current events, releases, figures, or facts requiring freshness; return concise, citable results.
            """)
    public WebSearchResult search(@Valid WebSearchRequest request) {
        // TODO: Check the request object to limit large max-results or max-chars in request by LLM
        log.debug("Web search tool invoked with request: {}",request);
        String query = request.query() == null ? "" : request.query().strip();
        if (query.length() < 3) {
            return WebSearchResult.failure("Query too short", query);
        }
        try {
            log.debug("Using web search provider for {}",request);
            WebSearchResult webSearchResponse = provider.search(request);
            log.debug("Web search provider returned {} results", webSearchResponse.results() == null ? 0 : webSearchResponse.results().size());
            int maxResults =
                    request.maxResults() != null ? request.maxResults() :
                            webSearchConfig.defaultMaxResults();
            int maxChars = request.maxSnippetChars() != null ?
                    request.maxSnippetChars() : 600;
            return webSearchResponse.truncatedTo(maxResults, maxChars);
        } catch (Exception e) {
            log.warn("Web search tool failed: {}", e.getMessage());
            return WebSearchResult.failure(
                    "Web search error: " + e.getMessage(), query);
        }
    }
}
