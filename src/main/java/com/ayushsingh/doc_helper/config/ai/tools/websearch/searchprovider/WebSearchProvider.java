package com.ayushsingh.doc_helper.config.ai.tools.websearch.searchprovider;

import com.ayushsingh.doc_helper.config.ai.tools.websearch.dto.WebSearchRequest;
import com.ayushsingh.doc_helper.config.ai.tools.websearch.dto.WebSearchResult;

public interface WebSearchProvider {
    WebSearchResult search(WebSearchRequest request);
}
