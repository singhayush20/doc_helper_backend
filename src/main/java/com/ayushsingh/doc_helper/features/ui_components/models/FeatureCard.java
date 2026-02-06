package com.ayushsingh.doc_helper.features.ui_components.models;

import com.ayushsingh.doc_helper.features.ui_components.actions.ComponentAction;

public record FeatureCard(TextInfo title, String iconUrl, String backgroundColor, ComponentAction onClick) implements UIComponent{
}
