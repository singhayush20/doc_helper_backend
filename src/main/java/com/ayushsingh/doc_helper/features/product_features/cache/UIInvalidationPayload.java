package com.ayushsingh.doc_helper.features.product_features.cache;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UIInvalidationPayload {
    Long featureId;
    String screen;
    Integer featureUIVersion;
}
