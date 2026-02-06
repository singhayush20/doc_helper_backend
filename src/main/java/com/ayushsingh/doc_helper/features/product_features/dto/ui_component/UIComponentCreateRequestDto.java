package com.ayushsingh.doc_helper.features.product_features.dto.ui_component;

import com.ayushsingh.doc_helper.features.product_features.entity.UIComponentType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UIComponentCreateRequestDto {

    private Long productFeatureId;
    private JsonNode uiConfig; // reading the uiConfig as JSON which would later be parsed to required type using a parser
    private UIComponentType uiComponentType; // this tells which type the uiConfig is
    private Integer version; // version is required to ensure that duplicate
    // conflicts or blind overwrites don't happen
    private Integer uiFeatureVersion;
    private String screen;
}
