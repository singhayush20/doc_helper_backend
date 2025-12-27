package com.ayushsingh.doc_helper.features.usage_monitoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UpdateTierRequest {
    @NotBlank(message = "User ID must not be blank")
    private Long userId;
    @NotEmpty(message = "Tier must not be empty")
    private String tier;
    @NotBlank(message = "Monthly limit must not be blank")
    private Long monthlyLimit;
}