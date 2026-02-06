package com.ayushsingh.doc_helper.features.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ayushsingh.doc_helper.features.chat.dto.ChatCallResponse;
import com.ayushsingh.doc_helper.features.chat.dto.ChatRequest;
import com.ayushsingh.doc_helper.features.chat.execution.DocChatCommand;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureExecutionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureDocChatController {

    private final FeatureExecutionService featureExecutionService;

    @PostMapping("/doc-chat")
    public ResponseEntity<ChatCallResponse> docChat(
            @RequestParam(name = "webSearch", defaultValue = "false") Boolean webSearch,
            @RequestBody ChatRequest request) {
        ChatCallResponse response = featureExecutionService.execute(
                FeatureCodes.DOC_CHAT,
                new DocChatCommand(request, webSearch));
        return ResponseEntity.ok(response);
    }
}
