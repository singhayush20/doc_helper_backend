package com.ayushsingh.doc_helper.core.ai.advisors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class PromptMetadataLoggingAdvisor implements CallAdvisor {

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    @NonNull
    public String getName() {
        return "PromptMetadataLoggingAdvisor";
    }

    @Override
    @NonNull
    public ChatClientResponse adviseCall(
            @NonNull ChatClientRequest chatClientRequest,
            @NonNull CallAdvisorChain callAdvisorChain) {
        Instant start = Instant.now();

        log.info("AI call started. Context keys: {}",
                chatClientRequest.context().keySet());

        if (log.isDebugEnabled()) {
            log.debug("Prompt messages count: {}",
                    chatClientRequest.prompt().getInstructions() != null
                            ? chatClientRequest.prompt().getInstructions().size()
                            : 0);
            if (chatClientRequest.prompt().getInstructions() != null) {
                chatClientRequest.prompt()
                        .getInstructions()
                        .forEach(message -> log.debug(
                                "Message type: {}, Content length: {}",
                                message.getMessageType(),
                                message.getText() != null
                                        ? message.getText().length()
                                        : 0));
            }
        }

        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse != null && chatResponse.getMetadata() != null) {
            Usage usage = chatResponse.getMetadata().getUsage();
            log.info(
                    "AI call completed. Model={}, DurationMs={}, PromptTokens={}, CompletionTokens={}, TotalTokens={}",
                    chatResponse.getMetadata().getModel(),
                    duration.toMillis(),
                    usage != null ? usage.getPromptTokens() : null,
                    usage != null ? usage.getCompletionTokens() : null,
                    usage != null ? usage.getTotalTokens() : null
            );
        } else {
            log.info("AI call completed. DurationMs={}, Metadata=absent",
                    duration.toMillis());
        }

        return response;
    }
}
