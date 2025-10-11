package com.ayushsingh.doc_helper.config.ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EmbeddingModelConfig {
    // private final OllamaEmbeddingModel ollamaEmbeddingModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;

    public EmbeddingModelConfig(OpenAiEmbeddingModel ollamaEmbeddingModel) {
        this.openAiEmbeddingModel = ollamaEmbeddingModel;
    }

    @Bean
    @Primary
    EmbeddingModel embeddingModel() {
        return openAiEmbeddingModel;
    }
}
