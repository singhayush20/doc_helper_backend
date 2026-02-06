package com.ayushsingh.doc_helper.features.chat.execution;

import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureExecutor;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureUsageMetrics;
import com.ayushsingh.doc_helper.features.product_features.guard.RequireFeature;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocChatExecutor implements FeatureExecutor<DocChatCommand, ChatCallResponse> {

    private final ChatService chatService;

    @Override
    public String featureCode() {
        return FeatureCodes.DOC_CHAT;
    }

    @Override
    @RequireFeature(code = FeatureCodes.DOC_CHAT, metric = FeatureUsageMetrics.REQUESTS, amount = "1")
    public ChatCallResponse execute(DocChatCommand input) {
        return chatService.generateResponse(input.request(), input.webSearch());
    }
}
