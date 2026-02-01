package com.ayushsingh.doc_helper.features.product_features.controller;

import com.ayushsingh.doc_helper.features.product_features.dto.*;
import com.ayushsingh.doc_helper.features.product_features.service.AdminFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/features")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminFeatureController {

    private final AdminFeatureService adminFeatureService;

    @PostMapping
    public ResponseEntity<ProductFeatureDto> create(
            @RequestBody FeatureCreateRequestDto featureCreateRequestDto
    ) {
        return ResponseEntity.ok(adminFeatureService.createFeature(featureCreateRequestDto));
    }

    @PutMapping("/{code}")
    public ResponseEntity<ProductFeatureDto> update(
            @PathVariable String code,
            @RequestBody FeatureUpdateRequestDto featureUpdateRequestDto
    ) {
        return ResponseEntity.ok(adminFeatureService.updateFeature(code, featureUpdateRequestDto));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        adminFeatureService.deleteFeature(code);
        return  ResponseEntity.noContent().build();
    }

    @PostMapping("/{featureCode}/enable")
    public ResponseEntity<Void> enableFeature(
            @PathVariable String featureCode
    ) {
        adminFeatureService.enableFeature(featureCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{featureCode}/disable")
    public ResponseEntity<Void> disableFeature(
            @PathVariable String featureCode
    ) {
        adminFeatureService.disableFeature(featureCode);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{featureCode}/ui")
    public ResponseEntity<FeatureUIConfigDto> updateUI(
            @PathVariable String featureCode,
            @RequestBody FeatureUIUpdateRequest request
    ) {
        var uiConfigDto = adminFeatureService.updateUI(featureCode, request);
        return  ResponseEntity.ok(uiConfigDto);
    }

    @PutMapping("/{featureCode}/action")
    public ResponseEntity<FeatureActionDto> updateAction(
            @PathVariable String featureCode,
            @RequestBody FeatureActionUpdateRequest request
    ) {
        var updatedAction = adminFeatureService.updateAction(featureCode, request);
        return ResponseEntity.ok(updatedAction);
    }
}
