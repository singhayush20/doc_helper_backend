package com.ayushsingh.doc_helper.features.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PasswordResetRequestDto {

    @NotBlank(message = "OTP cannot be blank")
    private final Integer otp;
    @Email(message = "Invalid email format")
    private final String email;
    @NotBlank(message = "New password cannot be blank")
    private final String newPassword;
}
