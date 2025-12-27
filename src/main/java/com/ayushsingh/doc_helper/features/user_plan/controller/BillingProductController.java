package com.ayushsingh.doc_helper.features.user_plan.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ayushsingh.doc_helper.features.user_plan.dto.*;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingProduct;
import com.ayushsingh.doc_helper.features.user_plan.service.BillingProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingProductController {

    private final BillingProductService billingProductService;

    @PostMapping("/products")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody CreateProductRequest request) {

        BillingProduct product = billingProductService.createProduct(request);
        return ResponseEntity.ok(mapProduct(product));
    }

    @PutMapping("/products/{productId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @RequestBody UpdateProductRequest request) {

        BillingProduct product = billingProductService.updateProduct(productId, request);
        return ResponseEntity.ok(mapProduct(product));
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
    public ResponseEntity<List<ProductResponse>> getActiveProducts() {
        List<ProductResponse> products = billingProductService
                .getAllActiveProducts()
                .stream()
                .map(this::mapProduct)
                .collect(Collectors.toList());

        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{productId}/prices")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<PriceResponse>> getPricesForProduct(
            @PathVariable Long productId) {

        List<PriceResponse> prices = billingProductService
                .getPricesForProduct(productId)
                .stream()
                .map(this::mapPrice)
                .collect(Collectors.toList());

        return ResponseEntity.ok(prices);
    }

    @GetMapping("/products/{productId}/prices/active")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<List<PriceResponse>> getAllActivePrices(
            @PathVariable Long productId) {

        List<PriceResponse> prices = billingProductService
                .getAllActivePrices(productId)
                .stream()
                .map(this::mapPrice)
                .collect(Collectors.toList());

        return ResponseEntity.ok(prices);
    }

    @PostMapping("/products/{productId}/prices")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<PriceResponse> addPriceToProduct(
            @PathVariable Long productId,
            @RequestBody CreatePriceRequest request) {

        BillingPrice price = billingProductService.addPriceToProduct(productId, request);
        return ResponseEntity.ok(mapPrice(price));
    }

    @DeleteMapping("/product/prices/{priceId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteBillingPriceFromProduct(@PathVariable Long priceId) {
        billingProductService.deleteBillingPriceFromProduct(priceId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/prices/{priceId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<PriceResponse> updatePrice(
            @PathVariable Long priceId,
            @RequestBody UpdatePriceRequest request) {

        BillingPrice price = billingProductService.updatePrice(priceId, request);
        return ResponseEntity.ok(mapPrice(price));
    }

    @PostMapping("/prices/{priceId}/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deactivatePrice(@PathVariable Long priceId) {
        billingProductService.deactivatePrice(priceId);
        return ResponseEntity.ok().build();
    }

    private ProductResponse mapProduct(BillingProduct product) {
        return ProductResponse.builder()
                .id(product.getId())
                .code(product.getCode())
                .displayName(product.getDisplayName())
                .tier(product.getTier())
                .monthlyTokenLimit(product.getMonthlyTokenLimit())
                .active(product.isActive())
                .build();
    }

    private PriceResponse mapPrice(BillingPrice price) {
        return PriceResponse.builder()
                .id(price.getId())
                .priceCode(price.getPriceCode())
                .billingPeriod(price.getBillingPeriod())
                .version(price.getVersion())
                .amount(price.getAmount().longValue())
                .currency(price.getCurrency())
                .providerPlanId(price.getProviderPlanId())
                .active(price.isActive())
                .build();
    }
}
