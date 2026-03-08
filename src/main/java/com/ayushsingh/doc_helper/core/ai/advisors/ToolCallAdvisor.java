package com.ayushsingh.doc_helper.core.ai.advisors;

import com.ayushsingh.doc_helper.core.ai.tools.websearch.dto.WebSearchItem;
import com.ayushsingh.doc_helper.core.ai.tools.websearch.dto.WebSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class ToolCallAdvisor implements BaseAdvisor {

    public static final String WEB_CITATIONS_KEY = "web_search_citations";
    private final ObjectMapper objectMapper;

    ToolCallAdvisor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public String getName() {
        return "ToolCallAdvisor";
    }

    @Override
    public ChatClientRequest before(@NonNull ChatClientRequest request,
                                    @NonNull AdvisorChain chain) {
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        List<WebSearchItem> citations = extractWebCitations(response);
        response.context().put(WEB_CITATIONS_KEY, citations);
        log.debug("ToolCallAdvisor (call) captured {} web citations", citations.size());
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest request,
                                                 @NonNull StreamAdvisorChain chain) {
        log.debug("Handling stream response for request: {}",request);
        return new ChatClientMessageAggregator()
                .aggregateChatClientResponse(
                        chain.nextStream(request),
                        aggregated -> {
                            log.debug("Extracting web search citations for " +
                                    "stream call for aggregated response: {}",
                                    aggregated);
                            List<WebSearchItem> citations = extractWebCitations(aggregated);
                            aggregated.context().put(WEB_CITATIONS_KEY, citations);
                            log.debug("ToolCallAdvisor (stream) captured {} web citations",
                                    citations.size());
                        }
                );
    }

    private List<WebSearchItem> extractWebCitations(ChatClientResponse response) {
        log.debug("Extracting web citations from: {}",response);
        if (response.chatResponse() == null) return List.of();

        Object raw = response.chatResponse().getMetadata().get("messages");
        if (!(raw instanceof List<?> messages)) return List.of();

        return messages.stream()
                .filter(m -> m instanceof ToolResponseMessage)
                .map(m -> (ToolResponseMessage) m)
                .flatMap(trm -> trm.getResponses().stream())
                .filter(r -> "web_search".equals(r.name()))
                .map(r -> parseItems(r.responseData()))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<WebSearchItem> parseItems(String json) {
        try {
            WebSearchResult result = objectMapper.readValue(json, WebSearchResult.class);
            return result.results() != null ? result.results() : List.of();
        } catch (Exception e) {
            log.warn("ToolCallAdvisor: failed to parse web_search result: {}", e.getMessage());
            return List.of();
        }
    }
}