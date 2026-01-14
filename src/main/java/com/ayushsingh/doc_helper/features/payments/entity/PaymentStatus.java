package com.ayushsingh.doc_helper.features.payments.entity;

/**
 * This enum is used to map business logic with the payment events
 * This should never be used with subscription events
 */
public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    REFUNDED
}