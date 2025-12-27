package com.ayushsingh.doc_helper.features.user_plan.service;

import java.util.List;

import com.ayushsingh.doc_helper.features.user_plan.dto.BillingPriceDetailsDto;
import com.ayushsingh.doc_helper.features.user_plan.dto.BillingPricesResponse;
import com.ayushsingh.doc_helper.features.user_plan.dto.BillingProductDetailsDto;
import com.ayushsingh.doc_helper.features.user_plan.dto.BillingProductsResponse;
import com.ayushsingh.doc_helper.features.user_plan.dto.CreatePriceRequest;
import com.ayushsingh.doc_helper.features.user_plan.dto.CreateProductRequest;
import com.ayushsingh.doc_helper.features.user_plan.dto.UpdatePriceRequest;
import com.ayushsingh.doc_helper.features.user_plan.dto.UpdateProductRequest;
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
}
