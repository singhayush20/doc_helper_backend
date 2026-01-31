package com.ayushsingh.doc_helper.features.product_features.controller;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.auth.entity.AuthUser;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureQueryService featureQueryService;

    @GetMapping("/home")
    public List<FeatureResponse> homeFeatures() {
        AuthUser user = UserContext.getCurrentUser();
        return featureQueryService.getHomeFeatures(user.getUser().getId());
    }
}

