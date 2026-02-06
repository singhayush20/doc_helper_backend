package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import com.ayushsingh.doc_helper.features.doc_summary.entity.SummaryLength;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SummaryChunker {

    private static final int MAX_TOKENS_PER_CHUNK = 1024;

    public List<String> split(String text, SummaryLength length) {
        int chunkSize;
        int overlap;

        switch (length) {
            case SHORT -> {
                chunkSize = 450;
                overlap = 50;
            }
            case MEDIUM -> {
                chunkSize = 700;
                overlap = 80;
            }
            case LONG -> {
                chunkSize = 1000;
                overlap = 120;
            }
            default -> {
                chunkSize = 700;
                overlap = 80;
            }
        }

        TokenTextSplitter splitter = new TokenTextSplitter(
                chunkSize,
                overlap,
                10,
                MAX_TOKENS_PER_CHUNK,
                true
        );

        List<Document> chunks = splitter.apply(List.of(new Document(text)));
        return chunks.stream().map(Document::getFormattedContent).toList();
    }
}
