package com.ayushsingh.doc_helper.features.user_plan.entity;

public enum SubscriptionStatus {
    INCOMPLETE, // new subscription is being initiated
    CREATED, // new subscription is activated but payment is not done yet
    ACTIVE, // currently active - payment is also done
    PAST_DUE, // payment not done
    CANCELED, // manually canceled
    HALTED, // halted due to payment issue and max payment retries exhausted
    EXPIRED, // subscription payments initiated but did not go through
}
