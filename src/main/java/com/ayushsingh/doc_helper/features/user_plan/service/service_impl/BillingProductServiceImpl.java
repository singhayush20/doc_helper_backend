package com.ayushsingh.doc_helper.features.user_plan.service.service_impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.user_plan.dto.CreatePriceRequest;
import com.ayushsingh.doc_helper.features.user_plan.dto.CreateProductRequest;
import com.ayushsingh.doc_helper.features.user_plan.dto.UpdatePriceRequest;
import com.ayushsingh.doc_helper.features.user_plan.dto.UpdateProductRequest;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingProduct;
import com.ayushsingh.doc_helper.features.user_plan.repository.BillingPriceRepository;
import com.ayushsingh.doc_helper.features.user_plan.repository.BillingProductRepository;
import com.ayushsingh.doc_helper.features.user_plan.service.BillingProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class BillingProductServiceImpl implements BillingProductService {

    private final BillingProductRepository productRepository;
    private final BillingPriceRepository priceRepository;

    // -------------------------------
    // PRODUCT MANAGEMENT
    // -------------------------------

    @Transactional
    @Override
    public BillingProduct createProduct(CreateProductRequest request) {
        BillingProduct product = BillingProduct.builder()
                .code(request.getCode())
                .displayName(request.getDisplayName())
                .tier(request.getTier())
                .monthlyTokenLimit(request.getMonthlyTokenLimit())
                .active(true)
                .build();

        return productRepository.save(product);
    }

    @Transactional
    @Override
    public BillingProduct updateProduct(Long productId, UpdateProductRequest request) {
        BillingProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new BaseException("Product not found", "PRODUCT_NOT_FOUND"));

        product.setDisplayName(request.getDisplayName());
        product.setTier(request.getTier());
        product.setMonthlyTokenLimit(request.getMonthlyTokenLimit());

        return productRepository.save(product);
    }

    @Transactional
    @Override
    public void activateProduct(Long productId) {
        BillingProduct product = productRepository.getReferenceById(productId);
        product.setActive(true);
    }

    @Transactional
    @Override
    public void deactivateProduct(Long productId) {
        BillingProduct product = productRepository.getReferenceById(productId);
        product.setActive(false);
    }

    @Transactional(readOnly = true)
    @Override
    public BillingProduct getProductByCode(String code) {
        return productRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new BaseException("Product not found", "PRODUCT_NOT_FOUND"));
    }

    @Transactional(readOnly = true)
    @Override
    public List<BillingProduct> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    // -------------------------------
    // PRICE MANAGEMENT
    // -------------------------------

    @Transactional(readOnly = true)
    @Override
    public List<BillingPrice> getPricesForProduct(Long productId) {
        return priceRepository.findByProductIdAndActiveTrue(productId);
    }

    @Transactional
    @Override
    public BillingPrice addPriceToProduct(Long productId, CreatePriceRequest request) {
        BillingProduct product = productRepository.getReferenceById(productId);

        BillingPrice price = BillingPrice.builder()
                .product(product)
                .priceCode(request.getPriceCode())
                .billingPeriod(request.getBillingPeriod())
                .version(request.getVersion())
                .amount(BigDecimal.valueOf(request.getAmount()))
                .currency(request.getCurrency())
                .providerPlanId(request.getProviderPlanId())
                .active(true)
                .build();

        return priceRepository.save(price);
    }

    @Transactional
    @Override
    public BillingPrice updatePrice(Long priceId, UpdatePriceRequest request) {
        BillingPrice price = priceRepository.findById(priceId)
                .orElseThrow(() -> new BaseException("Price not found", "PRICE_NOT_FOUND"));

        price.setAmount(BigDecimal.valueOf(request.getAmount()));
        price.setCurrency(request.getCurrency());

        return priceRepository.save(price);
    }

    @Transactional
    @Override
    public void deactivatePrice(Long priceId) {
        BillingPrice price = priceRepository.getReferenceById(priceId);
        price.setActive(false);
    }
}
