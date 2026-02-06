package com.ayushsingh.doc_helper.features.product_features.dto.ui_component;

import com.ayushsingh.doc_helper.features.product_features.entity.UIComponentType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UIComponentDetailsDto {

    private Long id;
    private UIComponentType uiComponentType;
    private JsonNode uiConfig;
    private boolean active;
    private String screen;
}
