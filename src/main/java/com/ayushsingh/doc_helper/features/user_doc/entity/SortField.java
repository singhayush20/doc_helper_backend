package com.ayushsingh.doc_helper.features.user_doc.entity;

public enum SortField {
    CREATED_AT("createdAt"),
    NAME("name");

    private final String fieldName;

    SortField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}