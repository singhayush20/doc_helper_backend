package com.ayushsingh.doc_helper.features.chat.dto;

import com.ayushsingh.doc_helper.features.chat.entity.ChatResponseCitation;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatCallResponse {
    private String message;
    private List<ChatResponseCitation> citations;
    private String errorMessage;
    private String errorCode;
}
