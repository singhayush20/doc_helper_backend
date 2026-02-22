package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.core.ai.advisors.PromptMetadataLoggingAdvisor;
import com.ayushsingh.doc_helper.features.doc_summary.dto.StructuredSummaryDto;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryLlmResponse;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryLlmService;
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

        @Value("${doc-summary.model}")
        String defaultModelName;

        @Value("${doc-summary.temperature}")
        Double temperature;

        public SummaryLlmServiceImpl(
                        ChatClient chatClient,
                        PromptMetadataLoggingAdvisor promptMetadataLoggingAdvisor) {
                this.chatClient = chatClient;
                this.promptMetadataLoggingAdvisor = promptMetadataLoggingAdvisor;
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

                StructuredSummaryDto structuredResponseEntity;
                ChatResponse chatResponse;

                try {
                        var response = clientResponse.responseEntity(StructuredSummaryDto.class);
                        structuredResponseEntity = response.getEntity();
                        chatResponse = response.getResponse();
                } catch (RuntimeException parseException) {
                        String message = parseException.getMessage() != null
                                        ? parseException.getMessage().toLowerCase()
                                        : "";

                        boolean likelyTruncated = message.contains("unexpected end-of-input")
                                        || message.contains("was expecting closing quote")
                                        || message.contains("end-of-input")
                                        || message.contains("jsonparseexception")
                                        || message.contains("jsonmappingexception");

                        if (likelyTruncated) {
                                throw new RuntimeException("INCOMPLETE_JSON_RESPONSE_FROM_MODEL", parseException);
                        }

                        throw new RuntimeException("INVALID_JSON_RESPONSE_FROM_MODEL", parseException);
                }

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
}
