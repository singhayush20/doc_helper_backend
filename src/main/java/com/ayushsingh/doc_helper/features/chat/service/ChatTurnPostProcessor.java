package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.features.usage_monitoring.dto.ChatContext;
import com.ayushsingh.doc_helper.features.user_activity.dto.ActivityTarget;
import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityType;
import com.ayushsingh.doc_helper.features.user_activity.service.UserActivityRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatTurnPostProcessor {

    private static final long SUMMARY_INTERVAL_TURNS = 6L;

    private final ChatSummaryService chatSummaryService;
    private final UserActivityRecorder userActivityRecorder;

    public void onAssistantResponseCompleted(ChatContext context, Long turnNumber) {
        triggerSummaryIfNeeded(context.chatThread().getId(), turnNumber);
        userActivityRecorder.record(
                context.userId(),
                new ActivityTarget(ActivityTargetType.USER_DOC, context.documentId()),
                UserActivityType.DOCUMENT_CHAT
        );
    }

    public void triggerSummaryIfNeeded(String threadId, Long turnNumber) {
        if (turnNumber % SUMMARY_INTERVAL_TURNS == 0) {
            chatSummaryService.summarizeThread(threadId);
        }
    }
}
