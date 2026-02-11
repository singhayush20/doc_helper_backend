package com.ayushsingh.doc_helper.core.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatModelConfig {

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

        @Primary
        public ChatClient docSummaryChatClient(
                        ChatClient.Builder builder) {

                return builder.build();
        }
}
