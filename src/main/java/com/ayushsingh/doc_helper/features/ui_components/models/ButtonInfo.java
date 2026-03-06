package com.ayushsingh.doc_helper.features.ui_components.models;

import com.ayushsingh.doc_helper.features.ui_components.actions.ComponentAction;

public record ButtonInfo(TextInfo buttonData, String leadingIcon, String trailingIcon, ComponentAction onClick,
        String buttonColor) implements UIComponent {
}
