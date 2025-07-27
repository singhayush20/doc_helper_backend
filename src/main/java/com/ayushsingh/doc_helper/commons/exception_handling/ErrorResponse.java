package com.ayushsingh.doc_helper.commons.exception_handling;

import java.time.Instant;

class ErrorResponse {
    private final String code;
    private final String message;
    private final Instant timestamp;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now();
    }
}
