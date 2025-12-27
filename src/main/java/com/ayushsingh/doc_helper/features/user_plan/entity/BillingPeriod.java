package com.ayushsingh.doc_helper.features.user_plan.entity;

public enum BillingPeriod {
    MONTHLY("monthly"),
    QUATERLY("quaterly"),
    YEARLY("yearly");

    BillingPeriod(String period) {
        this.period = period;
    }

    private final String period;

    public String getPeriod() {
        return period;
    }
}
