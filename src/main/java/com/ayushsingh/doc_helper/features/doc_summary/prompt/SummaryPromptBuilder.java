package com.ayushsingh.doc_helper.features.doc_summary.prompt;

import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryTone;

import java.util.List;

public final class SummaryPromptBuilder {

    public static String buildChunkPrompt(
            String chunk,
            SummaryTone tone,
            SummaryLength length) {

        return """
                You are a document summarization engine.

                TASK:
                Summarize the text below.

                TONE: %s
                LENGTH: %s

                STRICT RULES:
                - Return ONLY the summary.
                - Do NOT evaluate the text.
                - Do NOT comment on quality.
                - Do NOT address the reader.
                - Do NOT include introductions or conclusions.
                - Do NOT include phrases like "This summary..." or "In conclusion".
                - Do NOT add meta commentary.

                TEXT:
                %s
                """.formatted(tone.name(), length.name(), chunk);
    }

    public static String buildAggregatePrompt(
            List<String> summaries,
            SummaryTone tone,
            SummaryLength length,
            boolean finalPass) {

        String joined = String.join("\n\n---\n\n", summaries);

        return """
                You are a document summarization engine.

                TASK:
                Merge the summaries below into a single cohesive summary.

                TONE: %s
                LENGTH: %s

                STRICT RULES:
                - Return ONLY the final merged summary.
                - Do NOT evaluate.
                - Do NOT comment.
                - Do NOT mention tone or length.
                - Do NOT address the user.
                - Do NOT include phrases like "Good summary", "You've done", or "Thank you".
                - Do NOT explain what you are doing.
                - Output must begin directly with the summary content.

                SUMMARIES:
                %s
                """.formatted(tone.name(), length.name(), joined);
    }
}
