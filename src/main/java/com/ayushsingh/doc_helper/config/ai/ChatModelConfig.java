package com.ayushsingh.doc_helper.config.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
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
}
