package com.ayushsingh.doc_helper.features.payments.config;

import org.springframework.context.annotation.Configuration;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RazorPayConfig {

    private final PaymentGatewayConfig paymentGatewayConfig;

    RazorpayClient getRazorpayClient() throws RazorpayException {
        return new RazorpayClient(paymentGatewayConfig.getSecretKey(), paymentGatewayConfig.getKeyId());
    }
}
