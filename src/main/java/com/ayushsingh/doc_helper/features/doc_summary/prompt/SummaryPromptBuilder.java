package com.ayushsingh.doc_helper.features.doc_summary.prompt;

import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryTone;

import java.util.List;

public final class SummaryPromptBuilder {

    public static String buildChunkPrompt(String chunk, SummaryTone tone, SummaryLength length) {
        return """
                You are a summarization assistant.
                Tone: %s
                Length: %s

                Summarize the following text. Focus on factual accuracy and keep the summary self-contained.
                Text:
                %s
                """.formatted(tone.name(), length.name(), chunk);
    }

    public static String buildAggregatePrompt(List<String> summaries, SummaryTone tone, SummaryLength length, boolean finalPass) {
        String joined = String.join("\n\n---\n\n", summaries);

        String mode = finalPass
                ? "Produce the final summary from the summaries below."
                : "Combine and condense the summaries below into a smaller set of summaries.";

        return """
                You are a summarization assistant.
                Tone: %s
                Length: %s

                %s Keep the output coherent and avoid repetition.
                Summaries:
                %s
                """.formatted(tone.name(), length.name(), mode, joined);
    }
}
