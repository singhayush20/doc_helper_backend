package com.ayushsingh.doc_helper.features.doc_summary.service;

import java.util.List;

public interface DocumentChunkingService {
    List<String> splitWithoutOverlap(String text);

    List<String> splitWithOverlap(String text);
}
