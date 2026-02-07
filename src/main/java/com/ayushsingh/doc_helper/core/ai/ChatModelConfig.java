package com.ayushsingh.doc_helper.core.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatModelConfig {
    private final OpenAiChatModel openAiChatModel;

    public ChatModelConfig(OpenAiChatModel openAiChatModel) {
        this.openAiChatModel = openAiChatModel;
    }

    @Bean
    @Primary
    ChatModel chatModel() {
        return openAiChatModel;
    }

    @Bean("summarizerChatClient")
    public ChatClient summarizerChatClient(
            ChatClient.Builder builder,
            @Value("${summarizer.model}") String modelName) {

        OpenAiChatOptions summarizerOptions = OpenAiChatOptions.builder()
                .model(modelName)
                .temperature(0.2d)
                .maxTokens(300)
                .build();

        return builder
                .defaultOptions(summarizerOptions)
                .build();
    }

    @Bean("docSummaryChatClient")
    public ChatClient docSummaryChatClient(
            ChatClient.Builder builder,
            @Value("${doc-summary.model}") String modelName,
            @Value("${doc-summary.temperature:0.2}") double temperature,
            @Value("${doc-summary.default-max-tokens:300}") int defaultMaxTokens) {

        OpenAiChatOptions summarizerOptions = OpenAiChatOptions.builder()
                .model(modelName)
                .temperature(temperature)
                .maxTokens(defaultMaxTokens)
                .build();

        return builder
                .defaultOptions(summarizerOptions)
                .build();
    }

}
