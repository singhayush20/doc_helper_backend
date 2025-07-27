package com.ayushsingh.doc_helper.commons.exception_handling;

import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.DuplicateUserFoundException;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.RolesNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateUserFoundException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUserFoundException(DuplicateUserFoundException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage(), e.getCode()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RolesNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRolesNotFoundException(RolesNotFoundException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage(), e.getCode()), HttpStatus.NOT_FOUND);
    }

}
