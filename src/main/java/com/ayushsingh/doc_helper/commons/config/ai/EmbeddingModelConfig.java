package com.ayushsingh.doc_helper.commons.config.ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EmbeddingModelConfig {
    private final OllamaEmbeddingModel ollamaEmbeddingModel;

    public EmbeddingModelConfig(OllamaEmbeddingModel ollamaEmbeddingModel) {
        this.ollamaEmbeddingModel = ollamaEmbeddingModel;
    }

    @Bean
    @Primary
    EmbeddingModel embeddingModel() {
        return ollamaEmbeddingModel;
    }
}
