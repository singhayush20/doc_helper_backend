package com.ayushsingh.doc_helper.features.user.domain;

public enum RoleTypes {
    ADMIN("ROLE_ADMIN"),
    USER("ROLE_USER");

    final String value;

    RoleTypes(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
