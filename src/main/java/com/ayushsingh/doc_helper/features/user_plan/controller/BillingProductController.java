package com.ayushsingh.doc_helper.features.user_plan.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ayushsingh.doc_helper.features.user_plan.dto.*;
import com.ayushsingh.doc_helper.features.user_plan.service.BillingProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingProductController {

    private final BillingProductService billingProductService;

    @PostMapping("/products")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BillingProductDetailsDto> createProduct(
            @RequestBody CreateProductRequest request) {

        var product = billingProductService.createProduct(request);
        return ResponseEntity.ok(product);
    }

    @PutMapping("/products/{productId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BillingProductDetailsDto> updateProduct(
            @PathVariable Long productId,
            @RequestBody UpdateProductRequest request) {

        var product = billingProductService.updateProduct(productId, request);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        billingProductService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/products/{productId}/activate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> activateProduct(@PathVariable Long productId) {
        billingProductService.activateProduct(productId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/products/{productId}/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long productId) {
        billingProductService.deactivateProduct(productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/products/active")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<BillingProductsResponse> getActiveProducts() {
        var products = billingProductService
                .getAllActiveProducts();

        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{productId}/prices")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BillingPricesResponse> getPricesForProduct(
            @PathVariable Long productId) {

        var prices = billingProductService
                .getPricesForProduct(productId);

        return ResponseEntity.ok(prices);
    }

    @GetMapping("/products/{productId}/prices/active")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<BillingPricesResponse> getAllActivePrices(
            @PathVariable Long productId) {

        var prices = billingProductService
                .getAllActivePrices(productId);

        return ResponseEntity.ok(prices);
    }

    @PostMapping("/products/{productId}/prices")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BillingPriceDetailsDto> addPriceToProduct(
            @PathVariable Long productId,
            @RequestBody CreatePriceRequest request) {

        var price = billingProductService.addPriceToProduct(productId, request);
        return ResponseEntity.ok(price);
    }

    @DeleteMapping("/product/prices/{priceId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteBillingPriceFromProduct(@PathVariable Long priceId) {
        billingProductService.deleteBillingPriceFromProduct(priceId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/prices/{priceId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BillingPriceDetailsDto> updatePrice(
            @PathVariable Long priceId,
            @RequestBody UpdatePriceRequest request) {

        var price = billingProductService.updatePrice(priceId, request);
        return ResponseEntity.ok(price);
    }

    @PostMapping("/prices/{priceId}/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deactivatePrice(@PathVariable Long priceId) {
        billingProductService.deactivatePrice(priceId);
        return ResponseEntity.ok().build();
    }
}
