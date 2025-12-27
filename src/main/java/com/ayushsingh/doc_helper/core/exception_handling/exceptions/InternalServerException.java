package com.ayushsingh.doc_helper.core.exception_handling.exceptions;

import lombok.Getter;

@Getter
public class InternalServerException extends RuntimeException {
    private final String code;

    public InternalServerException(String message, String code) {
        super(message);
        this.code = code;
    }
}
