package com.ayushsingh.doc_helper.features.product_features.controller;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.auth.entity.AuthUser;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;
import com.ayushsingh.doc_helper.features.product_features.dto.ui.FeatureScreenResponse;
import com.ayushsingh.doc_helper.features.product_features.entity.UIComponentType;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureQueryService;
import com.ayushsingh.doc_helper.features.product_features.service.UIComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureQueryService featureQueryService;
    private final UIComponentService uiComponentService;

    // API to get all the enabled product features info - this does not
    // return the ui configs
    @GetMapping("/product-features")
    public FeatureResponse getProductFeatures() {
        AuthUser user = UserContext.getCurrentUser();
        return featureQueryService.getProductFeatures(user.getUser().getId());
    }

    // API to get all the enabled product features ui info - this returns the
    // ui configs
    @GetMapping("/ui-components")
    public FeatureScreenResponse getUIComponents(
            @RequestParam String screen,
            @RequestParam UIComponentType componentType
    ) {
        var user = UserContext.getCurrentUser().getUser();
        return uiComponentService.getUIFeatures(user.getId(), screen, componentType);
    }
}

