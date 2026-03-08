package com.ayushsingh.doc_helper.features.chat.service;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatPromptBuilder {

    public Prompt build(String historyContext,
                        String userQuestion,
                        List<Document> ragDocuments,
                        boolean webSearchEnabled) {
        StringBuilder numberedSources = new StringBuilder();
        for (int index = 0; index < ragDocuments.size(); index++) {
            numberedSources.append("[")
                    .append(index + 1)
                    .append("] ")
                    .append(ragDocuments.get(index).getFormattedContent())
                    .append("\n---\n");
        }

        String userPrompt = """
                ## Document Sources
                %s

                ## Conversation History
                %s

                ## Question
                %s

                """.formatted(numberedSources, historyContext, userQuestion);

        String systemPrompt = webSearchEnabled
                ? "You are an assistant that answers using the provided context. "
                + "If context is insufficient, call the web_search tool first and then "
                + "return the final answer grounded in the tool result. "
                + "Never output placeholder text like 'I will search the web and return later'."
                : "You are an assistant that answers using the provided context. "
                + "If context is insufficient, say that you do not know.";

        return new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        ));
    }
}
