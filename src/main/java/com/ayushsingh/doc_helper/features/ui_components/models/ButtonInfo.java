package com.ayushsingh.doc_helper.features.ui_components.models;

import com.ayushsingh.doc_helper.features.ui_components.actions.ComponentAction;

public record ButtonInfo(String text, String leadingIcon, String trailingIcon, ComponentAction onClick) implements UIComponent{
}
