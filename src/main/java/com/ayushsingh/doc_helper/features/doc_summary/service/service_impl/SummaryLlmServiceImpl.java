package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.core.ai.advisors.PromptMetadataLoggingAdvisor;
import com.ayushsingh.doc_helper.features.doc_summary.dto.StructuredSummaryDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryLlmResponse;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryLlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SummaryLlmServiceImpl implements SummaryLlmService {

        private final ChatClient chatClient;
        private final PromptMetadataLoggingAdvisor promptMetadataLoggingAdvisor;
        private final ObjectMapper objectMapper;

        @Value("${doc-summary.model}")
        String defaultModelName;

        @Value("${doc-summary.temperature}")
        Double temperature;

        public SummaryLlmServiceImpl(
                        ChatClient chatClient,
                        PromptMetadataLoggingAdvisor promptMetadataLoggingAdvisor,
                        ObjectMapper objectMapper) {
                this.chatClient = chatClient;
                this.promptMetadataLoggingAdvisor = promptMetadataLoggingAdvisor;
                this.objectMapper = objectMapper;
        }

        @Override
        public SummaryLlmResponse generate(String prompt, Integer maxTokens) {
                return generate(prompt, maxTokens, defaultModelName);
        }

        @Override
        public SummaryLlmResponse generate(String prompt, Integer maxTokens, String modelName) {
                log.debug("Generating summary response. model: {}, maxTokens: {}", modelName, maxTokens);

                var clientResponse = chatClient
                                .prompt(prompt)
                                .options(OpenAiChatOptions.builder()
                                                .model(modelName)
                                                .temperature(temperature)
                                                .maxTokens(maxTokens)
                                                .build())
                                .advisors(promptMetadataLoggingAdvisor)
                                .call();

                String rawText = clientResponse.content();
                String normalizedJson = normalizeToJson(rawText);

                StructuredSummaryDto structuredResponseEntity;
                try {
                        structuredResponseEntity = objectMapper.readValue(normalizedJson, StructuredSummaryDto.class);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }

                ChatResponse chatResponse = clientResponse.chatResponse();

                Usage usage = chatResponse != null && chatResponse.getMetadata() != null
                                ? chatResponse.getMetadata().getUsage()
                                : null;

                var promptTokens = usage != null ? usage.getPromptTokens() : null;
                var completionTokens = usage != null ? usage.getCompletionTokens() : null;
                var totalTokens = usage != null ? usage.getTotalTokens() : null;

                return new SummaryLlmResponse(
                                structuredResponseEntity,
                                promptTokens,
                                completionTokens,
                                totalTokens);
        }

        static String normalizeToJson(String rawText) {
                if (rawText == null) {
                        return "";
                }

                String normalized = rawText.trim();

                if (normalized.startsWith("```") || normalized.contains("```")) {
                        normalized = normalized
                                        .replace("```json", "")
                                        .replace("```JSON", "")
                                        .replace("```", "")
                                        .trim();
                }

                int objectStart = normalized.indexOf('{');
                int objectEnd = normalized.lastIndexOf('}');
                if (objectStart >= 0 && objectEnd > objectStart) {
                        normalized = normalized.substring(objectStart, objectEnd + 1);
                }

                return normalized.trim();
        }
}
