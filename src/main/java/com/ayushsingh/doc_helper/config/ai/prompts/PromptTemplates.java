package com.ayushsingh.doc_helper.config.ai.prompts;

public class PromptTemplates {

    public static final String RAG_PROMPT_TEMPLATE = """
            You are a helpful assistant for the document.
            Use the information provided in the "CONTEXT" section and the "CHAT HISTORY" to answer the user's question.
            If the answer is not available in the context or chat history, say "I do not have information about that."
            
            If the question is relevant but the context is insufficient, call the `webSearch` tool
            to retrieve supporting information before answering.
            
            CONTEXT:
            {context}
            
            CHAT HISTORY:
            {chatHistory}
            """;

    private PromptTemplates() {
    }
}
