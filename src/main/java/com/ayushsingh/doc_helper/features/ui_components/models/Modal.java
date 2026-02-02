package com.ayushsingh.doc_helper.features.ui_components.models;

public record Modal (TextInfo title, TextInfo description, ButtonInfo primaryButton, ButtonInfo secondaryButton, Boolean isCloseable) implements UIComponent{
}
