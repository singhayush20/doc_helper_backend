package com.ayushsingh.doc_helper.features.product_features.controller;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureCreateRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureUpdateRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ProductFeatureDto;
import com.ayushsingh.doc_helper.features.product_features.dto.feature_product.BillingProductFeatureDetailsDto;
import com.ayushsingh.doc_helper.features.product_features.dto.feature_product.BillingProductFeatureMapRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentCreateRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentDetailsDto;
import com.ayushsingh.doc_helper.features.product_features.service.AdminFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ProductFeatureDto> createNewFeature(
            @RequestBody FeatureCreateRequestDto featureCreateRequestDto
    ) {
        return ResponseEntity.ok(adminFeatureService.createFeature(featureCreateRequestDto));
    }

    @PutMapping("/{code}")
    public ResponseEntity<ProductFeatureDto> updateFeature(
            @PathVariable String code,
            @RequestBody FeatureUpdateRequestDto featureUpdateRequestDto
    ) {
        return ResponseEntity.ok(adminFeatureService.updateFeature(code, featureUpdateRequestDto));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteFeature(@PathVariable String code) {
        adminFeatureService.deleteFeature(code);
        return ResponseEntity.noContent().build();
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

    @PostMapping("/ui-component")
    public ResponseEntity<UIComponentDetailsDto> createUIComponentForFeature(
            @RequestBody UIComponentCreateRequestDto dto
    ) {
        var component = adminFeatureService.createUIComponent(dto);
        return new ResponseEntity<>(component, HttpStatus.CREATED);
    }

    @PostMapping("/product")
    public ResponseEntity<BillingProductFeatureDetailsDto> addFeatureToBillingProduct(
            @RequestBody BillingProductFeatureMapRequestDto dto
    ) {
        var mapping =
                adminFeatureService.addFeatureToBillingProduct(dto);
        return new ResponseEntity<>(mapping, HttpStatus.CREATED);
    }

    @PutMapping("/product-config")
    public ResponseEntity<BillingProductFeatureDetailsDto> updateProductFeatureConfig(
            @RequestBody BillingProductFeatureDetailsDto dto
    ) {
        var updated =
                adminFeatureService.updateProductFeatureConfig(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/product")
    public ResponseEntity<Void> removeFeatureFromBillingProduct(
            @RequestBody BillingProductFeatureMapRequestDto dto
    ) {
        adminFeatureService.removeFeatureFromBillingProduct(dto);
        return ResponseEntity.noContent().build();
    }
}
