package com.ayushsingh.doc_helper.core.ai.tools.websearch.webclient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.ayushsingh.doc_helper.core.ai.tools.websearch.WebSearchConfig;

@Configuration
@EnableConfigurationProperties(WebSearchConfig.class)
public class TavilyWebClientConfig {

    @Bean
    WebClient tavilyClient(WebClient.Builder builder) {
        // Increase in-memory buffer in case raw_content is large
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();

        return builder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .build();
    }
}
