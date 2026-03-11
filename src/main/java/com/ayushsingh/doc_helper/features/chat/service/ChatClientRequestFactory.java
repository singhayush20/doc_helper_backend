package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.core.ai.advisors.LoggingAdvisor;
import com.ayushsingh.doc_helper.core.ai.tools.websearch.WebSearchTool;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.ChatContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatClientRequestFactory {

    private final ChatClient chatClient;
    private final LoggingAdvisor loggingAdvisor;
    private final WebSearchTool webSearchTool;

    @Value("${doc-chat.model}")
    private String modelName;

    @Value("${doc-chat.temperature}")
    private Double temperature;

    public ChatClient.ChatClientRequestSpec build(ChatContext context,
                                                  boolean webSearchEnabled,
                                                  String generationId) {
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(modelName)
                .temperature(temperature);

        if (webSearchEnabled) {
            optionsBuilder.toolChoice(
                    OpenAiApi.ChatCompletionRequest.ToolChoiceBuilder.function("web_search")
            );
        }

        var requestSpec = chatClient.prompt(context.prompt())
                .options(optionsBuilder.build())
                .advisors(spec -> {
                    spec.param("documentId", context.documentId())
                            .param("userId", context.userId())
                            .param("threadId", context.chatThread().getId());
                    if (generationId != null) {
                        spec.param("generationId", generationId);
                    }
                })
                .advisors(new SimpleLoggerAdvisor())
                .advisors(loggingAdvisor);

        if (webSearchEnabled) {
            requestSpec.tools(webSearchTool);
        }
        return requestSpec;
    }
}
