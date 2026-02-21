package com.ayushsingh.doc_helper.features.doc_summary.prompt;

import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryTone;

import java.util.List;

public final class SummaryPromptBuilder {

    private SummaryPromptBuilder() {
    }

    public static String buildChunkPrompt(
            String chunk,
            SummaryTone tone,
            SummaryLength length) {

        return """
                You are a document summarization engine.

                TASK:
                Summarize the provided TEXT while preserving concrete facts and structure.

                STYLE REQUIREMENTS:
                %s
                %s

                OUTPUT FORMAT:
                Return ONLY valid JSON in this exact structure:

                {
                  "summary": "string",
                  "wordCount": integer
                }

                RULES:
                - summary must be markdown, not plain text.
                - summary must include:
                  1) a short heading line,
                  2) one concise overview paragraph,
                  3) a "## Key Points" section with 3-7 bullet points.
                - summary must respect the length constraint.
                - wordCount must reflect the actual word count of summary.
                - Do not add any fields.
                - Do not wrap JSON in markdown.
                - Never use triple backticks (```), markdown fences, or prose before/after JSON.
                - Escape all internal double quotes in summary as \\".
                - Encode line breaks inside summary as \\n.
                - Output must start with '{'.

                TEXT:
                %s
                """.formatted(
                toneInstruction(tone),
                chunkLengthInstruction(length),
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
                Merge the provided markdown summaries into one cohesive final summary.

                STYLE REQUIREMENTS:
                %s
                %s

                OUTPUT FORMAT:
                Return ONLY valid JSON in this exact structure:

                {
                  "summary": "string",
                  "wordCount": integer
                }

                RULES:
                - Eliminate repetition.
                - Maintain logical flow.
                - Preserve important names, numbers, claims, and outcomes.
                - summary must be markdown, not plain text.
                - summary must include:
                  1) a short heading line,
                  2) one overview paragraph,
                  3) a "## Key Points" section with bullet points.
                - wordCount must match summary.
                - No additional commentary.
                - Do not add any fields.
                - Do not wrap JSON in markdown.
                - Never use triple backticks (```), markdown fences, or prose before/after JSON.
                - Escape all internal double quotes in summary as \\".
                - Encode line breaks inside summary as \\n.
                - Output must start with '{'.

                SUMMARIES:
                %s
                """.formatted(
                toneInstruction(tone),
                finalLengthInstruction(length),
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

    private static String chunkLengthInstruction(SummaryLength length) {
        return switch (length) {
            case SHORT -> "Between 90 and 140 words.";
            case MEDIUM -> "Between 120 and 190 words.";
            case LONG -> "Between 180 and 280 words.";
            case VERY_LONG -> "Between 220 and 320 words.";
        };
    }

    private static String finalLengthInstruction(SummaryLength length) {
        return switch (length) {
            case SHORT -> "Maximum 150 words.";
            case MEDIUM -> "Between 150 and 300 words.";
            case LONG -> "Between 350 and 700 words.";
            case VERY_LONG -> "Between 700 and 1200 words.";
        };
    }
}
