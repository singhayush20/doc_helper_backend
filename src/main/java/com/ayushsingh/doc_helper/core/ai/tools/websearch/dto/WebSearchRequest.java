package com.ayushsingh.doc_helper.core.ai.tools.websearch.dto;

import java.util.List;
public record WebSearchRequest(
        String query,
        Integer maxResults,
        Integer maxSnippetChars,
        Integer daysBack,
        List<String> siteAllowList,
        List<String> siteDenyList) {
    public WebSearchRequest {
        if (daysBack == null)
            daysBack = 365;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query;
        private Integer maxResults;
        private Integer maxSnippetChars;
        private Integer daysBack;
        private List<String> siteAllowList;
        private List<String> siteDenyList;

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public Builder maxSnippetChars(Integer maxSnippetChars) {
            this.maxSnippetChars = maxSnippetChars;
            return this;
        }

        public Builder daysBack(Integer daysBack) {
            this.daysBack = daysBack;
            return this;
        }

        public Builder siteAllowList(List<String> siteAllowList) {
            this.siteAllowList = siteAllowList;
            return this;
        }

        public Builder siteDenyList(List<String> siteDenyList) {
            this.siteDenyList = siteDenyList;
            return this;
        }

        public WebSearchRequest build() {
            return new WebSearchRequest(query, maxResults, maxSnippetChars,
                    daysBack, siteAllowList, siteDenyList);
        }
    }

    
}
