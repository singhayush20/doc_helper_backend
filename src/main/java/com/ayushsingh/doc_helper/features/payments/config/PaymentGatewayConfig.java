package com.ayushsingh.doc_helper.features.payments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "payment.gateway")
@Getter
@Setter
@RequiredArgsConstructor
public class PaymentGatewayConfig {
    private final String keyId;
    private final String secretKey;
}
