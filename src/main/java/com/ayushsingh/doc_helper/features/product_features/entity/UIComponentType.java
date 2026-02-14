package com.ayushsingh.doc_helper.features.product_features.entity;

public enum UIComponentType {
    CARD("card"),
    BANNER("banner"),
    MODAL("modal"),
    DIALOG("dialog");

    private String value;

    UIComponentType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
