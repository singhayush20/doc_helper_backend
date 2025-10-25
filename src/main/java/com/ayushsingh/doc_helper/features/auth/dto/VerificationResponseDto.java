package com.ayushsingh.doc_helper.features.auth.dto;

public record VerificationResponseDto(
        boolean success,
        String email) {
}