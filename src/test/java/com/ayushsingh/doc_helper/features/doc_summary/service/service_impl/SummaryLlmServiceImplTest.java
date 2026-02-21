package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SummaryLlmServiceImplTest {

    @Test
    void normalizeToJson_stripsMarkdownFence() {
        String raw = """
                ```json
                {
                  \"summary\": \"# Title\\n\\n## Key Points\\n- a\",
                  \"wordCount\": 10
                }
                ```
                """;

        String normalized = SummaryLlmServiceImpl.normalizeToJson(raw);

        assertEquals("""
                {
                  \"summary\": \"# Title\\n\\n## Key Points\\n- a\",
                  \"wordCount\": 10
                }
                """.trim(), normalized);
    }

    @Test
    void normalizeToJson_extractsJsonFromPreambleAndSuffix() {
        String raw = "Here is the summary:\n{" +
                "\"summary\":\"# Head\\n\\n## Key Points\\n- x\",\"wordCount\":8}\nThanks";

        String normalized = SummaryLlmServiceImpl.normalizeToJson(raw);

        assertEquals("{\"summary\":\"# Head\\n\\n## Key Points\\n- x\",\"wordCount\":8}", normalized);
    }
}
