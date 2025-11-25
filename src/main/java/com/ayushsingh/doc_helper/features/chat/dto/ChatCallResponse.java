package com.ayushsingh.doc_helper.features.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatCallResponse {
    private String message;
    private String errorMessage;
    private String errorCode;
}
