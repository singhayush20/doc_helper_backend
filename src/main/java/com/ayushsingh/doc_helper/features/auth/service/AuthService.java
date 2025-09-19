package com.ayushsingh.doc_helper.features.auth.service;

import com.ayushsingh.doc_helper.features.auth.dto.EmailVerificationResponseDto;
import com.ayushsingh.doc_helper.features.auth.dto.EmailVerificationRequestDto;
import com.ayushsingh.doc_helper.features.auth.dto.PasswordResetRequestDto;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;

public interface AuthService {
    UserDetailsDto signUp(UserCreateDto signUpRequestDto);

    void sendEmailVerificationOtp(EmailVerificationRequestDto emailDto);

    EmailVerificationResponseDto verifyEmailOtp(EmailVerificationRequestDto emailDto);

    void sendPasswordResetOtp(EmailVerificationRequestDto emailDto);

    EmailVerificationResponseDto resetPassword(PasswordResetRequestDto emailDto);
}
