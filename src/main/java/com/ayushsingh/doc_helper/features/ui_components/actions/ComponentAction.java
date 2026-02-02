package com.ayushsingh.doc_helper.features.ui_components.actions;

import com.ayushsingh.doc_helper.features.ui_components.models.BottomSheet;
import com.ayushsingh.doc_helper.features.ui_components.models.Modal;

public record ComponentAction (String webViewUrl, String routeName, BottomSheet bottomSheet, Modal modal){
}
