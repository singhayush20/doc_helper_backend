package com.ayushsingh.doc_helper.commons.exception_handling.exceptions;

import javax.naming.AuthenticationException;

public class FirebaseAuthenticationException extends AuthenticationException {
    public FirebaseAuthenticationException(String message) {
        super(message);
    }
}
