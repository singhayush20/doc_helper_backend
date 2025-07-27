package com.ayushsingh.doc_helper.commons.exception_handling.exceptions;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import lombok.Getter;

@Getter
public class RolesNotFoundException extends RuntimeException {
    private final String code;

    public RolesNotFoundException(String message) {
        super(message);
        this.code = ExceptionCodes.ROLES_NOT_FOUND;
    }
}
