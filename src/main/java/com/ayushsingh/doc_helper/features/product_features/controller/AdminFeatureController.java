package com.ayushsingh.doc_helper.features.product_features.controller;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureActionUpdateRequest;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureUIUpdateRequest;
import com.ayushsingh.doc_helper.features.product_features.service.AdminFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/features")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeatureController {

    private final AdminFeatureService adminFeatureService;

    @PostMapping("/{featureCode}/enable")
    public void enableFeature(
            @PathVariable String featureCode
    ) {
        adminFeatureService.enableFeature(featureCode);
    }

    @PostMapping("/{featureCode}/disable")
    public void disableFeature(
            @PathVariable String featureCode
    ) {
        adminFeatureService.disableFeature(featureCode);
    }

    @PutMapping("/{featureCode}/ui")
    public void updateUI(
            @PathVariable String featureCode,
            @RequestBody FeatureUIUpdateRequest request
    ) {
        adminFeatureService.updateUI(featureCode, request);
    }

    @PutMapping("/{featureCode}/action")
    public void updateAction(
            @PathVariable String featureCode,
            @RequestBody FeatureActionUpdateRequest request
    ) {
        adminFeatureService.updateAction(featureCode, request);
    }
}
