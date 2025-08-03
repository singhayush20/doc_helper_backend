package com.ayushsingh.doc_helper.commons.exception_handling.exceptions;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import lombok.Getter;

@Getter
public class FirebaseAuthenticationException extends RuntimeException{

    private final String code;

    public FirebaseAuthenticationException(String message) {
        super(message);
        this.code = ExceptionCodes.FIREBASE_AUTH_EXCEPTION;
    }
}
