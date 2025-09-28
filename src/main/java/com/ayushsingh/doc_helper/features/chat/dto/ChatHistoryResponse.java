package com.ayushsingh.doc_helper.features.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ChatHistoryResponse {
    private  String threadId;
    private  List<ChatMessageResponse> messages;
}
