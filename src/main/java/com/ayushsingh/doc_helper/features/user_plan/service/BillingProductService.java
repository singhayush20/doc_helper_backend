package com.ayushsingh.doc_helper.features.user_plan.service;

import com.ayushsingh.doc_helper.features.user_plan.dto.*;
import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;

public interface BillingProductService {

    BillingProductDetailsDto createProduct(CreateProductRequest request);

    BillingProductDetailsDto updateProduct(Long productId, UpdateProductRequest request);

    void deleteProduct(Long productId);

    void activateProduct(Long productId);

    void deactivateProduct(Long productId);

    BillingProductsResponse getAllActiveProducts();

    BillingPricesResponse getPricesForProduct(Long productId);

    BillingPricesResponse getAllActivePrices(Long productId);

    BillingPriceDetailsDto addPriceToProduct(Long productId, CreatePriceRequest request);

    BillingPriceDetailsDto updatePrice(Long priceId, UpdatePriceRequest request);

    void deactivatePrice(Long priceId);

    void deleteBillingPriceFromProduct(Long priceId);

    Long getProductIdByTier(AccountTier tier);
}
