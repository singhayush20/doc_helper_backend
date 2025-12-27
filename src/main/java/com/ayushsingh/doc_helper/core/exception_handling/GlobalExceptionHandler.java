package com.ayushsingh.doc_helper.core.exception_handling;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        private ResponseEntity<ErrorResponse> buildResponse(Exception e,
                        HttpStatus status) {

                log.error("Exception caught: {}", e.getMessage(), e);

                if (e instanceof BaseException customEx) {
                        return new ResponseEntity<>(new ErrorResponse(customEx.getCode(),
                                        customEx.getMessage()), status);
                }
                return new ResponseEntity<>(
                                new ErrorResponse("UNKNOWN_ERROR", e.getMessage()),
                        status);
        }

        @ExceptionHandler(BaseException.class)
        public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
                return buildResponse(e, getHttpStatus(e));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                        MethodArgumentNotValidException e) {
                StringBuilder message = new StringBuilder();
                e.getBindingResult().getFieldErrors().forEach(error -> 
                    message.append(error.getField())
                          .append(": ")
                          .append(error.getDefaultMessage())
                          .append("; ")
                );
                return new ResponseEntity<>(
                    new ErrorResponse("VALIDATION_ERROR", message.toString().trim()),
                    HttpStatus.BAD_REQUEST
                );
        }

        private HttpStatus getHttpStatus(BaseException e) {
                return switch (e.getCode()) {
                        case ExceptionCodes.DUPLICATE_USER_FOUND -> HttpStatus.CONFLICT;
                        case ExceptionCodes.EMAIL_MISMATCH -> HttpStatus.BAD_REQUEST;
                        case ExceptionCodes.FIREBASE_AUTH_EXCEPTION -> HttpStatus.UNAUTHORIZED;
                        case ExceptionCodes.ROLES_NOT_FOUND -> HttpStatus.NOT_FOUND;
                        case ExceptionCodes.USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
                        case ExceptionCodes.INVALID_OTP -> HttpStatus.BAD_REQUEST;
                        default -> HttpStatus.INTERNAL_SERVER_ERROR;
                };
        }
}
