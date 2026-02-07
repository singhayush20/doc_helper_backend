package com.ayushsingh.doc_helper.features.doc_summary.service.service_impl;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SummaryChunker {

    private static final int MAX_TOKENS_PER_CHUNK = 4096;
    private static final String TABLE_START = "[TABLE_START]";
    private static final String TABLE_END = "[TABLE_END]";

    public List<String> splitForStorage(String text) {
        // For RAG - WITHOUT overlap for context
        return splitWithConfig(text, 1000, 0);
    }
    
    public List<String> splitWithOverlap(String text) {
        // For summarization - WITH overlap for context
        return splitWithConfig(text, 800, 100);
    }

    private List<String> splitWithConfig(String text, int chunkSize, int overlap) {
        String normalized = normalizePreserveStructure(text);
        List<String> blocks = splitIntoBlocks(normalized);
        blocks = mergeTables(blocks);
        blocks = mergeHeadings(blocks);
        String merged = String.join("\n\n", blocks);

        TokenTextSplitter splitter = new TokenTextSplitter(
                chunkSize,
                overlap,
                10,
                MAX_TOKENS_PER_CHUNK,
                true
        );

        List<Document> chunks = splitter.apply(List.of(new Document(merged)));
        return chunks.stream()
                .map(Document::getFormattedContent)
                .filter(s -> s != null && !s.isBlank())
                .toList();
    }

    private String normalizePreserveStructure(String text) {
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        normalized = normalized.replaceAll("[ \\t]+", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    private List<String> splitIntoBlocks(String text) {
        String[] parts = text.split("\\n\\n+");
        List<String> blocks = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                blocks.add(trimmed);
            }
        }
        return blocks;
    }

    private List<String> mergeHeadings(List<String> blocks) {
        List<String> merged = new ArrayList<>();
        int i = 0;
        while (i < blocks.size()) {
            String current = blocks.get(i);
            if (isHeading(current) && i + 1 < blocks.size()) {
                merged.add(current + "\n" + blocks.get(i + 1));
                i += 2;
                continue;
            }
            merged.add(current);
            i++;
        }
        return merged;
    }

    private List<String> mergeTables(List<String> blocks) {
        List<String> merged = new ArrayList<>();
        StringBuilder tableBuilder = null;
        for (String block : blocks) {
            if (TABLE_START.equals(block)) {
                tableBuilder = new StringBuilder();
                tableBuilder.append(TABLE_START).append("\n");
                continue;
            }
            if (TABLE_END.equals(block)) {
                if (tableBuilder != null) {
                    tableBuilder.append(TABLE_END);
                    merged.add(tableBuilder.toString());
                    tableBuilder = null;
                } else {
                    merged.add(block);
                }
                continue;
            }
            if (tableBuilder != null) {
                tableBuilder.append(block).append("\n");
            } else {
                merged.add(block);
            }
        }
        if (tableBuilder != null) {
            merged.add(tableBuilder.toString().trim());
        }
        return merged;
    }

    private boolean isHeading(String block) {
        if (block.length() > 100)
            return false;

        if (block.equals(TABLE_START) || block.equals(TABLE_END)) {
            return false;
        }

        // Markdown headings
        if (block.matches("^#{1,6}\\s+.+"))
            return true;

        // Ends with colon
        if (block.endsWith(":"))
            return true;

        // Numbered headings
        if (block.matches("^\\d+\\.\\s+.+"))
            return true;

        // All caps (your existing logic)
        String lettersOnly = block.replaceAll("[^A-Za-z]", "");
        if (!lettersOnly.isEmpty() && lettersOnly.equals(lettersOnly.toUpperCase())) {
            return true;
        }

        // Short single-line blocks are likely headings
        return !block.contains("\n") && block.length() < 50;
    }
}
