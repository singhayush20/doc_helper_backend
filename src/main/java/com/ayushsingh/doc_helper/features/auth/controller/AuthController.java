package com.ayushsingh.doc_helper.features.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.ayushsingh.doc_helper.features.auth.dto.EmailVerificationRequestDto;
import com.ayushsingh.doc_helper.features.auth.dto.VerificationResponseDto;
import com.ayushsingh.doc_helper.features.auth.dto.PasswordResetRequestDto;
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

    @PostMapping("/email/otp")
    public ResponseEntity<Void> sendEmailVerificationOtp(@RequestBody EmailVerificationRequestDto emailDto) {
        authService.sendEmailVerificationOtp(emailDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/verify-otp")
    public ResponseEntity<VerificationResponseDto> verifyEmailOtp(
            @RequestBody EmailVerificationRequestDto emailDto) {
        var response = authService.verifyEmailOtp(emailDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/otp")
    public ResponseEntity<Void> sendPasswordResetOtp(@Valid @RequestBody EmailVerificationRequestDto emailDto) {
        authService.sendPasswordResetOtp(emailDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<VerificationResponseDto> resetPassword(
            @RequestBody PasswordResetRequestDto passwordDto) {
        var response = authService.resetPassword(passwordDto);
        return ResponseEntity.ok(response);
    }
}