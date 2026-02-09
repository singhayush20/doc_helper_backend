package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.doc_summary.dto.SummaryLlmResponse;
import com.ayushsingh.doc_helper.features.doc_summary.service.SummaryLlmService;

@Component
public class SummaryLlmServiceImpl implements SummaryLlmService{

    private final ChatClient docSummaryChatClient;
    private final String docSummaryModel;
    private final double docSummaryTemperature;

    public SummaryLlmServiceImpl(
            @Qualifier("docSummaryChatClient") ChatClient docSummaryChatClient,
            @Value("${doc-summary.model}") String docSummaryModel,
            @Value("${doc-summary.temperature:0.2}") double docSummaryTemperature
    ) {
        this.docSummaryChatClient = docSummaryChatClient;
        this.docSummaryModel = docSummaryModel;
        this.docSummaryTemperature = docSummaryTemperature;
    }

    @Override
    public SummaryLlmResponse generate(String prompt, Integer maxTokens) {
        var spec = docSummaryChatClient.prompt(prompt);
        if (maxTokens != null) {
            spec.options(OpenAiChatOptions.builder()
                    .model(docSummaryModel)
                    .temperature(docSummaryTemperature)
                    .maxTokens(maxTokens)
                    .build());
        }
        ChatClient.CallResponseSpec response = spec.call();
        ChatResponse chatResponse = response.chatResponse();

        Usage usage = chatResponse != null && chatResponse.getMetadata() != null
                ? chatResponse.getMetadata().getUsage()
                : null;

        var promptTokens = usage != null ? usage.getPromptTokens() : null;
        var completionTokens = usage != null ? usage.getCompletionTokens() : null;
        var totalTokens = usage != null ? usage.getTotalTokens() : null;

        return new SummaryLlmResponse(
                response.content(),
                promptTokens,
                completionTokens,
                totalTokens
        );
    }
}
