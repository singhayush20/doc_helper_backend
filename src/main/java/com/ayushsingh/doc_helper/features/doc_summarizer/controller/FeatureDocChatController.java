package com.ayushsingh.doc_helper.features.doc_summarizer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.doc_summarizer.executor_command.DocChatCommand;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureExecutionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/summarizer")
@RequiredArgsConstructor
public class FeatureDocChatController {

    private final FeatureExecutionService featureExecutionService;

    @PostMapping("/doc")
    public ResponseEntity<ChatCallResponse> docChat(
            @RequestBody ChatRequest request) {
        ChatCallResponse response = featureExecutionService.execute(
                FeatureCodes.DOC_CHAT,
                new DocChatCommand(request));
        return ResponseEntity.ok(response);
    }
}
