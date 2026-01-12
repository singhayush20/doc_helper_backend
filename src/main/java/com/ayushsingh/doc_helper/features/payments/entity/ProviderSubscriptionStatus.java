package com.ayushsingh.doc_helper.features.payments.entity;

public enum ProviderSubscriptionStatus {
    CREATED, // Razorpay: created
    AUTHENTICATED, // Razorpay: authenticated
    ACTIVE, // Razorpay: active
    PENDING, // Razorpay: pending
    HALTED, // Razorpay: halted
    CANCELLED, // Razorpay: cancelled
    EXPIRED, // Razorpay: expired
    COMPLETED, // Razorpay: completed
    UNKNOWN
}
