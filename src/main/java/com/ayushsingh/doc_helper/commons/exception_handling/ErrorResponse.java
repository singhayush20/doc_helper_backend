package com.ayushsingh.doc_helper.commons.exception_handling;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ErrorResponse {
    private final String code;
    private final String message;
    private final Instant timestamp;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now();
    }
}
