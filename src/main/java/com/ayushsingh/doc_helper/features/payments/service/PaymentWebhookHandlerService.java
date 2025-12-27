package com.ayushsingh.doc_helper.features.payments.service;

public interface PaymentWebhookHandlerService {

    void handleProviderEvent(String rawPayload, String signatureHeader);
}