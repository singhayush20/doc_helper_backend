package com.ayushsingh.doc_helper.features.payments.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RazorPayConfig {

    private final RazorpayProperties paymentGatewayConfig;

    @Bean
    RazorpayClient getRazorpayClient() throws RazorpayException {
        return new RazorpayClient(paymentGatewayConfig.keyId(), paymentGatewayConfig.secretKey());
    }
}
