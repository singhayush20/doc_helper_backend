package com.ayushsingh.doc_helper.features.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionResponse {
    private String providerSubscriptionId;
    private String providerKeyId;
    private String planCode;
    private String priceCode;
}