package com.ayushsingh.doc_helper.features.auth.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class EmailVerificationRequestDto {
    @Email(message = "Invalid email format")
    private String email;

    private String otp;
}
