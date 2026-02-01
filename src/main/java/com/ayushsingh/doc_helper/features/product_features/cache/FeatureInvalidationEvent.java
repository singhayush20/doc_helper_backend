package com.ayushsingh.doc_helper.features.product_features.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureInvalidationEvent {

    private InvalidationType type;

    /**
     * Optional – when null, invalidate globally
     */
    private Long userId;
}
