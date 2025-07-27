package com.ayushsingh.doc_helper.commons.exception_handling.exceptions;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import lombok.Getter;

@Getter
public class DuplicateUserFoundException extends RuntimeException {
    private final String code;

    public DuplicateUserFoundException(String message) {
        super(message);
        this.code = ExceptionCodes.DUPLICATE_USER_FOUND;
    }
}
