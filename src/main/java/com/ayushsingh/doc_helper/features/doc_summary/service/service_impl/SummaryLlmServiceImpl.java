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
                } catch (RuntimeException firstParseException) {
                        log.warn("Primary structured parse failed. Retrying with strict JSON prompt for model {}", modelName);

                        String repairedPrompt = buildRepairPrompt(prompt);

                        try {
                                var repairedClientResponse = chatClient
                                                .prompt(repairedPrompt)
                                                .options(OpenAiChatOptions.builder()
                                                                .model(modelName)
                                                                .temperature(0.0)
                                                                .maxTokens(Math.max(220, maxTokens))
                                                                .build())
                                                .advisors(promptMetadataLoggingAdvisor)
                                                .call();

                                var repairedResponse = repairedClientResponse.responseEntity(StructuredSummaryDto.class);
                                structuredResponseEntity = repairedResponse.getEntity();
                                chatResponse = repairedResponse.getResponse();
                        } catch (RuntimeException repairException) {
                                String message = repairException.getMessage() != null
                                                ? repairException.getMessage().toLowerCase()
                                                : "";

                                boolean likelyTruncated = message.contains("unexpected end-of-input")
                                                || message.contains("was expecting closing quote")
                                                || message.contains("end-of-input")
                                                || message.contains("jsonparseexception")
                                                || message.contains("jsonmappingexception");

                                if (likelyTruncated) {
                                        throw new RuntimeException("INCOMPLETE_JSON_RESPONSE_FROM_MODEL", repairException);
                                }

                                throw new RuntimeException("INVALID_JSON_RESPONSE_FROM_MODEL", repairException);
                        }
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

        private String buildRepairPrompt(String originalPrompt) {
                return """
                                You are a strict JSON generation assistant.

                                TASK:
                                Generate valid JSON for this exact schema:
                                {
                                  "summary": "string",
                                  "wordCount": integer
                                }

                                RULES:
                                - Follow the original summarization instructions exactly.
                                - Ensure summary is markdown text.
                                - wordCount must match summary content.
                                - Return ONLY valid JSON.
                                - Do not wrap JSON in markdown.
                                - Do not include any explanation.

                                ORIGINAL_INSTRUCTIONS:
                                %s
                                """.formatted(originalPrompt == null ? "" : originalPrompt);
        }
}
