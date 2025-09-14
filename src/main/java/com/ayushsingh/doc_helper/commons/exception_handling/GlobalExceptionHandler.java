package com.ayushsingh.doc_helper.commons.exception_handling;

import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(
            GlobalExceptionHandler.class);
    private static final Map<String, HttpStatus> CODE_TO_STATUS = Map.of(
            ExceptionCodes.DUPLICATE_USER_FOUND, HttpStatus.CONFLICT,
            ExceptionCodes.EMAIL_MISMATCH, HttpStatus.BAD_REQUEST,
            ExceptionCodes.FIREBASE_AUTH_EXCEPTION, HttpStatus.UNAUTHORIZED,
            ExceptionCodes.ROLES_NOT_FOUND, HttpStatus.NOT_FOUND
    );

    private ResponseEntity<ErrorResponse> buildResponse(RuntimeException e,
            HttpStatus status) {

        // Log exception with MDC context automatically included
        log.error("Exception caught: {}", e.getMessage(), e);

        // Build error response
        if (e instanceof BaseException customEx) {
            return new ResponseEntity<>(new ErrorResponse(customEx.getMessage(),
                    customEx.getCode()), status);
        }
        return new ResponseEntity<>(
                new ErrorResponse(e.getMessage(), "UNKNOWN_ERROR"), status);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        return buildResponse(e, getHttpStatus(e));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unhandled exception: {}", e.getMessage(), e);
        return new ResponseEntity<>(
                new ErrorResponse("Internal server error", "UNKNOWN_ERROR"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus getHttpStatus(BaseException e) {
        return CODE_TO_STATUS.getOrDefault(e.getCode(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
