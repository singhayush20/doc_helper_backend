package com.ayushsingh.doc_helper.features.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ayushsingh.doc_helper.features.auth.service.AuthService;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDetailsDto> signupUser(@RequestBody UserCreateDto userCreateDto) {
        var userDetailsDto = this.authService.signUp(userCreateDto);
        return ResponseEntity.ok(userDetailsDto);
    }

    // TODO: Create endpoint for email verification

    // TODO: Create endpoint for password reset

    // TODO: No endpoint for sign-in as it will be handled by Firebase directly, on
    // the frontend
}

// https://chatgpt.com/share/68cc5a23-13f8-8006-8442-4eaef3ce9ce5
