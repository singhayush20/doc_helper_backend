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
                                        .append("\n\n---\n\n");
                }

                String userPrompt = """
                                ## Retrieved Documents
                                The following documents were retrieved from the knowledge base.
                                Each document has an index that must be used for citation.

                                %s

                                ## Conversation History
                                %s

                                ## User Question
                                %s

                                Use the documents above when possible.
                                """.formatted(numberedSources, historyContext, userQuestion);

                String systemPrompt = webSearchEnabled
                                ? """
                                                You are a professional AI assistant that answers questions using provided documents.

                                                Follow these rules strictly:

                                                1. Prefer answering using the retrieved documents.
                                                2. If the documents do NOT contain enough information,
                                                   call the `web_search` tool to retrieve information.
                                                3. Do NOT guess or hallucinate information.
                                                4. After calling a tool, incorporate the results into the final answer.
                                                5. Never output placeholder text like:
                                                   "I will search the web and return later."

                                                -------------------------
                                                CITATION RULES
                                                -------------------------

                                                • When using retrieved documents, cite them using their index.

                                                Example:
                                                Climate change is accelerating due to greenhouse gas emissions [2].

                                                • When using web sources, cite them as Markdown links.

                                                Example:
                                                - [NASA Climate Report](https://climate.nasa.gov)

                                                -------------------------
                                                RESPONSE FORMAT
                                                -------------------------

                                                Always produce the final answer in this structure:

                                                ### Answer
                                                <clear and well-structured explanation>

                                                ### Sources
                                                **Documents**
                                                - [1]
                                                - [3]

                                                **Web Sources**
                                                - [Title](URL)

                                                If no web sources were used, omit that section.
                                                If no documents were used, omit that section.

                                                The answer should be concise, structured, and easy to read.
                                                """
                                : """
                                                You are a professional AI assistant that answers questions using only the provided documents.

                                                Rules:
                                                1. Use ONLY the retrieved documents.
                                                2. Do NOT invent information not present in the documents.
                                                3. If the documents do not contain the answer, say:
                                                   "The provided documents do not contain enough information to answer this question."

                                                -------------------------
                                                CITATION RULES
                                                -------------------------

                                                Cite documents using their index.

                                                Example:
                                                Climate change is accelerating due to greenhouse gas emissions [2].

                                                -------------------------
                                                RESPONSE FORMAT
                                                -------------------------

                                                ### Answer
                                                <clear explanation>

                                                ### Sources
                                                **Documents**
                                                - [1]
                                                - [3]
                                                """;

                return new Prompt(List.of(
                                new SystemMessage(systemPrompt),
                                new UserMessage(userPrompt)));
        }
}