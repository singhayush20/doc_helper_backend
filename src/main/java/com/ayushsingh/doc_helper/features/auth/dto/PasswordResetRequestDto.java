package com.ayushsingh.doc_helper.features.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestDto {

    @NotBlank(message = "OTP cannot be blank")
    private String otp;
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "New password cannot be blank")
    private String newPassword;
}
