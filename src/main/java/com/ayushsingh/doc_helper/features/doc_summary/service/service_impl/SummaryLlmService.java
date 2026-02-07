package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SummaryLlmService {

    private final ChatClient.Builder chatClientBuilder;

    public String generate(String prompt) {
        ChatClient client = chatClientBuilder.build();
        return client.prompt(prompt).call().content();
    }
}
