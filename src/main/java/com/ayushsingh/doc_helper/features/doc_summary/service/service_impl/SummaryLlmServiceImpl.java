package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.core.ai.advisors.PromptMetadataLoggingAdvisor;
import com.ayushsingh.doc_helper.features.doc_summary.dto.StructuredSummaryDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryLlmResponse;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryLlmService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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
                        PromptMetadataLoggingAdvisor promptMetadataLoggingAdvisor, ObjectMapper objectMapper) {
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
                                                // .maxTokens(maxTokens) // TODO: Re-enable maxTokens - to ensure we get response within limit
                                                .build())
                                .advisors(promptMetadataLoggingAdvisor)
                                .call();

                var chatClientResponse = clientResponse.chatClientResponse();
                var chatResponse = chatClientResponse.chatResponse();
                var rawContent = chatResponse.getResult().getOutput().getText();
                System.out.println("Raw LLM response content: " + rawContent);

                StructuredSummaryDto structuredResponseEntity;

                // 2️⃣ Strict JSON parse with truncation detection
                try {
                        structuredResponseEntity = objectMapper.readValue(rawContent, StructuredSummaryDto.class);

                } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {

                        String message = ex.getMessage() != null
                                        ? ex.getMessage().toLowerCase()
                                        : "";

                        boolean likelyTruncated = message.contains("unexpected end-of-input")
                                        || message.contains("was expecting closing quote")
                                        || message.contains("end-of-input")
                                        || message.contains("jsonparseexception")
                                        || message.contains("jsonmappingexception");

                        if (likelyTruncated) {
                                throw new RuntimeException(
                                                "INCOMPLETE_JSON_RESPONSE_FROM_MODEL",
                                                ex);
                        }

                        throw new RuntimeException(
                                        "INVALID_JSON_RESPONSE_FROM_MODEL",
                                        ex);
                }

                var usage = chatResponse.getMetadata().getUsage();
                log.info("Summary generation successful. Prompt tokens: {}, Completion tokens: {}, Total tokens: {}",
                                usage.getPromptTokens(),
                                usage.getCompletionTokens(),
                                usage.getTotalTokens());
                return new SummaryLlmResponse(
                                structuredResponseEntity,
                                usage.getPromptTokens(),
                                usage.getCompletionTokens(),
                                usage.getTotalTokens());
        }
}
