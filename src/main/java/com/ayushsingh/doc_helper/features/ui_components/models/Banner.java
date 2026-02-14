package com.ayushsingh.doc_helper.features.ui_components.models;

import com.ayushsingh.doc_helper.features.ui_components.actions.ComponentAction;

public record Banner(TextInfo title, TextInfo description, String leadingImageUrl, ButtonInfo buttonInfo,
        ComponentAction onClick)
        implements UIComponent {
}
