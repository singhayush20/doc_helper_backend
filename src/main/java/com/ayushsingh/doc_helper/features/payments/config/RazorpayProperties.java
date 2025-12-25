package com.ayushsingh.doc_helper.features.payments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.gateway")
public record RazorpayProperties(
        String keyId,
        String secretKey,
        String webhookSecret) {
}
