package com.ayushsingh.doc_helper.features.auth.dto;

public record EmailVerificationResponseDto(
        boolean success,
        String email) {
}