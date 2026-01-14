package com.ayushsingh.doc_helper.features.user_plan.entity;

public enum SubscriptionStatus {
    INCOMPLETE, // initiated but not transacted
    ACTIVE, // currently active
    PAST_DUE, // payment not done
    CANCELED, // manually canceled
    HALTED, // halted due to payment issue and max payment retries exhausted
    EXPIRED, // subscription payments initiated but did not go through
}
