package com.ayushsingh.doc_helper.features.doc_summarizer.executor;

import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.doc_summarizer.executor_command.DocChatCommand;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureExecutor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocSummaryExecutor implements FeatureExecutor<DocChatCommand, ChatCallResponse> {

    @Override
    public String featureCode() {
        return FeatureCodes.DOC_CHAT;
    }

    @Override
    public ChatCallResponse execute(DocChatCommand input) {
        // TODO: Implement doc chat for the ocr document

        return null;
    }
}
