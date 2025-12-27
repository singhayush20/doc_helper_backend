package com.ayushsingh.doc_helper.core.ai.tools.websearch.searchprovider;

import com.ayushsingh.doc_helper.core.ai.tools.websearch.dto.WebSearchRequest;
import com.ayushsingh.doc_helper.core.ai.tools.websearch.dto.WebSearchResult;

public interface WebSearchProvider {
    WebSearchResult search(WebSearchRequest request);
}
