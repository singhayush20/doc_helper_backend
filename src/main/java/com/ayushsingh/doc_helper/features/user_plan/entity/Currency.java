package com.ayushsingh.doc_helper.features.user_plan.entity;

public enum Currency {
    INR("INR");

    Currency(String code) {
        this.code = code;
    }

    private final String code;

    public String getCode() {
        return code;
    }
}
