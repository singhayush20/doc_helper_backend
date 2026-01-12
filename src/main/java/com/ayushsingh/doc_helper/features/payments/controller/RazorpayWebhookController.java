package com.ayushsingh.doc_helper.features.payments.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ayushsingh.doc_helper.features.payments.service.PaymentWebhookHandlerService;

@RestController
@RequestMapping("/api/v1/billing/razorpay")
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookController {

    private final PaymentWebhookHandlerService webhookHandlerService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        log.info("Received webhook trigger from Razorpay {}",payload);
        webhookHandlerService.handleProviderEvent(payload, signature);
        return ResponseEntity.ok().build();
    }
}
