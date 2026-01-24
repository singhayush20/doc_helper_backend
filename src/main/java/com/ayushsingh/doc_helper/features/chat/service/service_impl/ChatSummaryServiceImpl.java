package com.ayushsingh.doc_helper.features.chat.service.service_impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ayushsingh.doc_helper.features.chat.entity.ChatMessage;
import com.ayushsingh.doc_helper.features.chat.entity.ChatThread;
import com.ayushsingh.doc_helper.features.chat.repository.ChatMessageRepository;
import com.ayushsingh.doc_helper.features.chat.repository.ChatThreadRepository;
import com.ayushsingh.doc_helper.features.chat.service.ChatSummaryService;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class ChatSummaryServiceImpl implements ChatSummaryService {

    private static final String SUMMARY_CACHE_PREFIX = "thread:summary:";

    private final ChatClient summarizerChatClient;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public ChatSummaryServiceImpl(
            @Qualifier("summarizerChatClient") ChatClient summarizerChatClient,
            ChatThreadRepository chatThreadRepository,
            ChatMessageRepository chatMessageRepository,
            RedisTemplate<String, String> redisTemplate) {

        this.summarizerChatClient = summarizerChatClient;
        this.chatThreadRepository = chatThreadRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.redisTemplate = redisTemplate;
    }

    private static final String SUMMARY_PROMPT = """
            You are maintaining a running summary of a conversation.
            
            Existing summary:
            {oldSummary}
            
            New conversation turns:
            {newTurns}
            
            Update the summary. Keep it concise and focused on user intent, decisions,
            and important facts needed for future responses.
            """;

    @Async
    @Override
    public void summarizeThread(String threadId) {
        log.info("Summarizing thread {}", threadId);
        ChatThread thread = chatThreadRepository.findById(threadId).orElse(null);
        if (thread == null) {
            return;
        }

        String oldSummary = thread.getConversationSummary();

        List<ChatMessage> lastTurns = chatMessageRepository.findLastNTurns(threadId, 6);
        log.info("Last 6 turns for thread id: {}, number of turns found: {}", threadId, lastTurns.size());

        if (lastTurns.isEmpty()) {
            return;

        }
        String newTurns = lastTurns.stream()
                .map(m -> m.getRole().getValue() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = SUMMARY_PROMPT
                .replace("{oldSummary}", oldSummary == null ? "None" : oldSummary)
                .replace("{newTurns}", newTurns);
        log.info("Generating summary for threadId: {}", threadId);
        try {
            String updatedSummary = summarizerChatClient
                    .prompt(new Prompt(List.of(new UserMessage(prompt))))
                    .call()
                    .content();

            if (updatedSummary == null || updatedSummary.isBlank()) {
                log.warn("Summarizer returned empty summary for thread {}", threadId);
                return;
            }
            log.info("Generated summary for threadId: {}, summary length: {}", threadId, updatedSummary.length());

            thread.setConversationSummary(updatedSummary);
            thread.setSummaryUpdatedAt(Instant.now());
            chatThreadRepository.save(thread);

            redisTemplate.opsForValue().set(
                    SUMMARY_CACHE_PREFIX + threadId,
                    updatedSummary,
                    Duration.ofHours(6));


            log.info("Updated summary for thread {}", threadId);

        } catch (Exception e) {
            log.error("Failed to summarize thread {}", threadId, e);
        }
    }

    @Override
    public String getCachedSummary(ChatThread thread) {

        String key = SUMMARY_CACHE_PREFIX + thread.getId();
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null)
            return cached;

        if (thread.getConversationSummary() != null) {
            redisTemplate.opsForValue().set(
                    key,
                    thread.getConversationSummary(),
                    Duration.ofHours(6));
            return thread.getConversationSummary();
        }

        return null;
    }
}
