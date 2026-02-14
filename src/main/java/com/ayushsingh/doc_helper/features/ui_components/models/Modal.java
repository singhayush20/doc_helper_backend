package com.ayushsingh.doc_helper.features.ui_components.models;

public record Modal (TextInfo title, TextInfo description, ButtonInfo primaryButton, ButtonInfo secondaryButton, Boolean isClosable, String iconUrl) implements UIComponent{
}
