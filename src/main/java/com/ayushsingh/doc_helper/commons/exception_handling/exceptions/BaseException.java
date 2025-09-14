package com.ayushsingh.doc_helper.commons.exception_handling.exceptions;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final String code;

    public BaseException(String message, String code) {
        super(message);
        this.code = code;
    }
}
