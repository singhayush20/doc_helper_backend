package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.core.ai.advisors.PromptMetadataLoggingAdvisor;
import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryLlmResponse;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryLlmService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SummaryLlmServiceImpl implements SummaryLlmService {

        private final ChatClient chatClient;
        private final PromptMetadataLoggingAdvisor promptMetadataLoggingAdvisor;

        public SummaryLlmServiceImpl(
                @Qualifier("docSummaryChatClient") ChatClient chatClient,
                        PromptMetadataLoggingAdvisor promptMetadataLoggingAdvisor) {
                this.chatClient = chatClient;
                this.promptMetadataLoggingAdvisor = promptMetadataLoggingAdvisor;
        }

        @Override
        public SummaryLlmResponse generate(String prompt, Integer maxTokens) {
                log.debug("Generating response for prompt: {} \n maxTokens: {}", prompt, maxTokens);

                var spec = chatClient
                                .prompt(prompt)
                                .options(OpenAiChatOptions.builder()
                                                .maxTokens(maxTokens)
                                                .build())
                                .advisors(promptMetadataLoggingAdvisor);

                var response = spec.call();
                ChatResponse chatResponse = response.chatResponse();

                Usage usage = chatResponse != null && chatResponse.getMetadata() != null
                                ? chatResponse.getMetadata().getUsage()
                                : null;

                var promptTokens = usage != null ? usage.getPromptTokens() : null;
                var completionTokens = usage != null ? usage.getCompletionTokens() : null;
                var totalTokens = usage != null ? usage.getTotalTokens() : null;

                return new SummaryLlmResponse(
                                (chatResponse != null) ? chatResponse.getResult().getOutput().getText() : Strings.EMPTY,
                                promptTokens,
                                completionTokens,
                                totalTokens);
        }
}
