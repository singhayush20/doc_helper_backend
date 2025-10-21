package com.ayushsingh.doc_helper.config.ai.tools.websearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "web.search")
public record WebSearchConfig(String baseUrl, String apiKey, int timeoutSeconds,
                              int defaultMaxResults) {
}
