package com.ayushsingh.doc_helper.features.product_features.guard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// must be placed on the boundary where a feature is executed, not on read APIs.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireFeature {

    String code();
    String metric();
    String amount();
}

