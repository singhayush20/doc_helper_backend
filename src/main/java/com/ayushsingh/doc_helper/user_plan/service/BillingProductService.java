package com.ayushsingh.doc_helper.user_plan.service;

import java.util.List;

import com.ayushsingh.doc_helper.user_plan.dto.CreatePriceRequest;
import com.ayushsingh.doc_helper.user_plan.dto.CreateProductRequest;
import com.ayushsingh.doc_helper.user_plan.dto.UpdatePriceRequest;
import com.ayushsingh.doc_helper.user_plan.dto.UpdateProductRequest;
import com.ayushsingh.doc_helper.user_plan.entity.BillingPrice;
import com.ayushsingh.doc_helper.user_plan.entity.BillingProduct;

public interface BillingProductService {

    BillingProduct createProduct(CreateProductRequest request);

    BillingProduct updateProduct(Long productId, UpdateProductRequest request);

    void activateProduct(Long productId);

    void deactivateProduct(Long productId);

    BillingProduct getProductByCode(String code);

    List<BillingProduct> getAllActiveProducts();

    List<BillingPrice> getPricesForProduct(Long productId);

    BillingPrice addPriceToProduct(Long productId, CreatePriceRequest request);

    BillingPrice updatePrice(Long priceId, UpdatePriceRequest request);

    void deactivatePrice(Long priceId);
}
