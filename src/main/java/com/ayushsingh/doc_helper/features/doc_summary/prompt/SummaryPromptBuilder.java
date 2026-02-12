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
                Summarize the provided TEXT.

                STYLE REQUIREMENTS:
                %s
                %s

                OUTPUT FORMAT:
                Return ONLY valid JSON in this exact structure:

                {
                  "summary": "string",
                  "wordCount": integer,
                }

                RULES:
                - summary must respect the length constraint.
                - wordCount must reflect the actual word count of summary.
                - keyPoints must contain 3–7 bullet insights extracted from the text.
                - Do not add any fields.
                - Do not wrap JSON in markdown.
                - Output must start with '{'.

                TEXT:
                %s
                """.formatted(
                toneInstruction(tone),
                lengthInstruction(length),
                chunk);
    }

    public static String buildAggregatePrompt(
            List<String> summaries,
            SummaryTone tone,
            SummaryLength length) {

        String joined = String.join("\n\n---\n\n", summaries);

        return """
                You are a document summarization engine.

                TASK:
                Merge the provided summaries into one cohesive final summary.

                STYLE REQUIREMENTS:
                %s
                %s

                OUTPUT FORMAT:
                Return ONLY valid JSON:

                {
                  "summary": "string",
                  "wordCount": integer,
                }

                RULES:
                - Eliminate repetition.
                - Maintain logical flow.
                - keyPoints must reflect the merged content.
                - wordCount must match summary.
                - No additional commentary.
                - Output must start with '{'.

                SUMMARIES:
                %s
                """.formatted(
                toneInstruction(tone),
                lengthInstruction(length),
                joined);
    }

    private static String toneInstruction(SummaryTone tone) {
        return switch (tone) {
            case PROFESSIONAL -> "Use formal, neutral business language.";
            case CASUAL -> "Use simple, conversational language.";
            case EXECUTIVE -> "Use concise, high-impact executive-level language.";
            case TECHNICAL -> "Use precise domain-specific terminology.";
            case LEGAL -> "Use formal legal-style language with precise wording.";
        };
    }

    private static String lengthInstruction(SummaryLength length) {
        return switch (length) {
            case SHORT -> "Maximum 150 words.";
            case MEDIUM -> "Between 150 and 300 words.";
            case LONG -> "Between 300 and 600 words.";
            case VERY_LONG -> "Between 600 and 1000 words.";
        };
    }
}
