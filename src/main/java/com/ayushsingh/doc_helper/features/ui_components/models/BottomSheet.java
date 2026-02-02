package com.ayushsingh.doc_helper.features.ui_components.models;

public record BottomSheet(TextInfo title, TextInfo description, String iconUrl, ButtonInfo primaryButton,
                          ButtonInfo secondaryButton, Boolean isClosable) implements UIComponent{

}
