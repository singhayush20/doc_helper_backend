package com.ayushsingh.doc_helper.core.ai.tools.websearch.searchprovider.searchproviderimpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.ayushsingh.doc_helper.core.ai.tools.websearch.WebSearchConfig;
import com.ayushsingh.doc_helper.core.ai.tools.websearch.dto.*;
import com.ayushsingh.doc_helper.core.ai.tools.websearch.searchprovider.WebSearchProvider;

import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class TavilyWebSearchProvider implements WebSearchProvider {

    private final WebClient tavilyClient;
    private final WebSearchConfig props;

    // Simple HTML stripper for raw content; replace with Jsoup if you prefer
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    @Override
    @Cacheable(cacheNames = "webSearch", key = "#request.query() + '|' + #request.maxResults() + '|' + #request.daysBack() + '|' + T(java.util.Objects).hash(#request.siteAllowList()) + '|' + T(java.util.Objects).hash(#request.siteDenyList())")
    public WebSearchResult search(WebSearchRequest request) {
        log.debug("Using tavily search tool for request: {}",request);
        TavilyRequest payload = new TavilyRequest(
                request.query(),
                false, // auto_parameters
                "general", // topic
                "basic", // search_depth
                3, // chunks_per_source
                coalesce(request.maxResults(), props.defaultMaxResults()),
                null, // time_range
                coalesce(request.daysBack(), 365),
                true, // include_answer
                true, // include_raw_content
                false, // include_images
                false, // include_image_descriptions
                false, // include_favicon
                request.siteAllowList(),
                request.siteDenyList(),
                null // country
        );

        TavilyResponse tavilyResponse;
        try {
            tavilyResponse = tavilyClient.post()
                    .uri(props.baseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(TavilyResponse.class)
                    .timeout(Duration.ofSeconds(props.timeoutSeconds()))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(300))
                            .filter(this::isRetryable))
                    .block();
        if(tavilyResponse.results() != null ) {
            log.debug("Tavily Search Response results length: {}", tavilyResponse.results().size());
        }
        else {
            log.debug("Tavily Search Response results is null");
        }
        } catch (Exception e) {
            log.warn("Tavily request failed: {}", e.getMessage());
            return WebSearchResult.failure("Web search error: " + e.getMessage(), request.query());
        }

        if (tavilyResponse == null || tavilyResponse.results() == null || tavilyResponse.results().isEmpty()) {
            log.debug("No Response in Tavily Search {}", tavilyResponse);
            return WebSearchResult.failure("No results", request.query());
        }

        // Map, normalize, de-duplicate, sort, and clip
        List<WebSearchItem> items = tavilyResponse.results().stream()
                .filter(Objects::nonNull)
                .map(it -> WebSearchItem.builder()
                        .title(nz(it.title()))
                        .url(canonical(it.url()))
                        .snippet(clip(stripHtml(nz(it.content())),
                                coalesce(request.maxSnippetChars(), 600)))
                        .source(host(it.url()))
                        .publishedAt(nz(it.published_date()))
                        .score(it.score())
                        .build())
                .filter(i -> i.url() != null && !i.url().isBlank())
                .distinct()
                .sorted(Comparator.comparing(WebSearchItem::score,
                        Comparator.nullsLast(Double::compareTo)).reversed())
                .limit(coalesce(request.maxResults(), props.defaultMaxResults()))
                .toList();

        return WebSearchResult.builder()
                .success(true)
                .message("OK")
                .query(tavilyResponse.query())
                .answer(nz(tavilyResponse.answer()))
                .requestId(nz(tavilyResponse.request_id()))
                .results(items)
                .build();
    }

    private boolean isRetryable(Throwable t) {
        // Retry on transient I/O/timeout; WebClient maps many errors to
        // WebClientRequestException
        return true;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String stripHtml(String s) {
        // Removes simple tags; preserves text for token-friendly snippets
        return TAGS.matcher(s).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private static String clip(String s, int max) {
        if (s == null)
            return "";
        if (s.length() <= max)
            return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String canonical(String url) {
        try {
            if (url == null)
                return null;
            URI u = URI.create(url);
            String host = u.getHost();
            String path = u.getPath() == null ? "" : u.getPath();
            // Drop query fragments/UTM to stabilize citations
            return (u.getScheme() == null ? "https" : u.getScheme()) + "://" + host + path;
        } catch (Exception e) {
            return url;
        }
    }

    private static String host(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer coalesce(Integer v, Integer d) {
        return v != null ? v : d;
    }
}
