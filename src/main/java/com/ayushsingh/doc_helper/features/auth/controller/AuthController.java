package com.ayushsingh.doc_helper.features.auth.controller;

import com.ayushsingh.doc_helper.commons.constants.AuthConstants;
import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.auth.service.AuthService;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDetailsDto> signupUser(@RequestBody UserCreateDto userCreateDto,
            HttpServletRequest request) {
        var authorizationHeader = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (authorizationHeader != null && authorizationHeader.startsWith(AuthConstants.BEARER)) {
            var firebaseToken = authorizationHeader.substring(7);
            var userDetailsDto = this.authService.signUp(userCreateDto, firebaseToken);
            return ResponseEntity.ok(userDetailsDto);
        } else
            throw new BaseException("Token not found", ExceptionCodes.FIREBASE_AUTH_EXCEPTION);
    }
}
