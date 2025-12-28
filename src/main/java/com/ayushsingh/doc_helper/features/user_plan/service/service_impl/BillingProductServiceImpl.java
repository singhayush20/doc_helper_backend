package com.ayushsingh.doc_helper.features.user_plan.service.service_impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.payments.config.RazorpayProperties;
import com.ayushsingh.doc_helper.features.payments.service.PaymentProviderClient;
import com.ayushsingh.doc_helper.features.user_plan.dto.BillingPriceDetailsDto;
import com.ayushsingh.doc_helper.features.user_plan.dto.BillingPricesResponse;
import com.ayushsingh.doc_helper.features.user_plan.dto.BillingProductDetailsDto;
import com.ayushsingh.doc_helper.features.user_plan.dto.BillingProductsResponse;
import com.ayushsingh.doc_helper.features.user_plan.dto.CreatePriceRequest;
import com.ayushsingh.doc_helper.features.user_plan.dto.CreateProductRequest;
import com.ayushsingh.doc_helper.features.user_plan.dto.UpdatePriceRequest;
import com.ayushsingh.doc_helper.features.user_plan.dto.UpdateProductRequest;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingPrice;
import com.ayushsingh.doc_helper.features.user_plan.entity.BillingProduct;
import com.ayushsingh.doc_helper.features.user_plan.entity.SubscriptionStatus;
import com.ayushsingh.doc_helper.features.user_plan.repository.BillingPriceRepository;
import com.ayushsingh.doc_helper.features.user_plan.repository.BillingProductRepository;
import com.ayushsingh.doc_helper.features.user_plan.repository.SubscriptionRepository;
import com.ayushsingh.doc_helper.features.user_plan.service.BillingProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingProductServiceImpl implements BillingProductService {

        private final BillingProductRepository productRepository;
        private final BillingPriceRepository priceRepository;
        private final SubscriptionRepository subscriptionRepository;
        private final PaymentProviderClient paymentProviderClient;
        private final RazorpayProperties razorpayProperties;

        @Transactional
        @Override
        public BillingProductDetailsDto createProduct(CreateProductRequest request) {
                BillingProduct product = BillingProduct.builder()
                                .code(request.getCode())
                                .displayName(request.getDisplayName())
                                .tier(request.getTier())
                                .monthlyTokenLimit(request.getMonthlyTokenLimit())
                                .active(true)
                                .build();

                return mapProduct(productRepository.save(product));
        }

        @Transactional
        @Override
        public BillingProductDetailsDto updateProduct(Long productId, UpdateProductRequest request) {
                BillingProduct product = productRepository.findById(productId)
                                .orElseThrow(() -> new BaseException("Product not found",
                                                ExceptionCodes.PRODUCT_NOT_FOUND));

                product.setDisplayName(request.getDisplayName());
                product.setTier(request.getTier());
                product.setMonthlyTokenLimit(request.getMonthlyTokenLimit());

                return mapProduct(productRepository.save(product));
        }

        @Transactional
        @Override
        public void deleteProduct(Long productId) {

                BillingProduct product = productRepository.findById(productId)
                                .orElseThrow(() -> new BaseException(
                                                "Product not found",
                                                ExceptionCodes.PRODUCT_NOT_FOUND));

                // Must be inactive
                if (product.isActive()) {
                        throw new BaseException(
                                        "Active product cannot be deleted",
                                        ExceptionCodes.ACTIVE_PRODUCT_DELETION);
                }

                // Must NOT have any prices
                boolean hasPrices = priceRepository.existsByProductId(productId);
                if (hasPrices) {
                        throw new BaseException(
                                        "Product has pricing history and cannot be deleted",
                                        ExceptionCodes.BILLING_PRODUCT_IN_USE);
                }

                // Must NOT have subscription history
                boolean hasSubscriptions = subscriptionRepository.existsSubscriptionsForProductWithStatuses(
                                productId,
                                List.of(
                                                SubscriptionStatus.ACTIVE,
                                                SubscriptionStatus.PAST_DUE,
                                                SubscriptionStatus.CANCELED,
                                                SubscriptionStatus.EXPIRED,
                                                SubscriptionStatus.HALTED));

                if (hasSubscriptions) {
                        throw new BaseException(
                                        "Product has subscription history and cannot be deleted",
                                        ExceptionCodes.ACTIVE_SUBSCRIPTION_PRODUCT_DELETION);
                }

                productRepository.delete(product);
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

                BillingProduct product = productRepository.findById(productId)
                                .orElseThrow(() -> new BaseException(
                                                "Product not found", ExceptionCodes.PRODUCT_NOT_FOUND));

                boolean hasActiveSubscriptions = subscriptionRepository.existsSubscriptionsForProductWithStatuses(
                                productId,
                                List.of(
                                                SubscriptionStatus.ACTIVE,
                                                SubscriptionStatus.PAST_DUE));

                if (hasActiveSubscriptions) {
                        throw new BaseException(
                                        "Cannot deactivate product with active subscriptions",
                                        "PRODUCT_IN_USE");
                }

                product.setActive(false);
                productRepository.save(product);
        }

        @Transactional(readOnly = true)
        @Override
        public BillingProductsResponse getAllActiveProducts() {
                var products = productRepository.findByActiveTrue();
                return new BillingProductsResponse(products.stream()
                                .map(this::mapProduct)
                                .collect(Collectors.toList()));

        }

        @Transactional(readOnly = true)
        @Override
        public BillingPricesResponse getPricesForProduct(Long productId) {
                var prices = priceRepository.findByProductId(productId);

                return new BillingPricesResponse(prices.stream()
                                .map(this::mapPrice)
                                .collect(Collectors.toList()));
        }

        @Transactional
        @Override
        public BillingPriceDetailsDto addPriceToProduct(Long productId, CreatePriceRequest request) {
                BillingProduct product = productRepository.getReferenceById(productId);
                System.out
                                .println("Razorpay Key: " + razorpayProperties.keyId() + " Secret: "
                                                + razorpayProperties.secretKey());

                var providerPlanId = paymentProviderClient.createPlan(
                                request.getBillingPeriod(),
                                BigDecimal.valueOf(request.getAmount()),
                                product.getCode(),
                                request.getCurrency().getCode(),
                                request.getPriceCode(),
                                request.getDescription(),
                                request.getVersion());

                BillingPrice price = BillingPrice.builder()
                                .product(product)
                                .priceCode(request.getPriceCode())
                                .billingPeriod(request.getBillingPeriod())
                                .version(request.getVersion())
                                .amount(BigDecimal.valueOf(request.getAmount()))
                                .currency(request.getCurrency())
                                .providerPlanId(providerPlanId)
                                .description(request.getDescription())
                                .active(false)
                                .build();

                return mapPrice(priceRepository.save(price));
        }

        @Transactional
        @Override
        public BillingPriceDetailsDto updatePrice(Long priceId, UpdatePriceRequest request) {
                BillingPrice price = priceRepository.findById(priceId)
                                .orElseThrow(() -> new BaseException("Price not found",
                                                ExceptionCodes.PRICE_NOT_FOUND));

                price.setAmount(BigDecimal.valueOf(request.getAmount()));
                price.setCurrency(request.getCurrency());

                return mapPrice(priceRepository.save(price));
        }

        @Transactional
        @Override
        public void deactivatePrice(Long priceId) {
                BillingPrice price = priceRepository.getReferenceById(priceId);
                price.setActive(false);
        }

        @Transactional
        @Override
        public void deleteBillingPriceFromProduct(Long priceId) {

                BillingPrice price = priceRepository.findById(priceId)
                                .orElseThrow(() -> new BaseException(
                                                "Billing price not found",
                                                ExceptionCodes.PRICE_NOT_FOUND));

                // 1. Must be inactive
                if (price.isActive()) {
                        throw new BaseException(
                                        "Deactivate price before deletion",
                                        ExceptionCodes.ACTIVE_SUBSCRIPTION_PRICE_DELETION);
                }

                // 2. Must have no subscriptions
                boolean hasSubscriptions = subscriptionRepository
                                .existsSubscriptionsForPriceWithStatuses(
                                                priceId,
                                                List.of(
                                                                SubscriptionStatus.INCOMPLETE,
                                                                SubscriptionStatus.ACTIVE,
                                                                SubscriptionStatus.PAST_DUE,
                                                                SubscriptionStatus.HALTED));

                if (hasSubscriptions) {
                        throw new BaseException(
                                        "Cannot delete price with existing subscriptions",
                                        ExceptionCodes.ACTIVE_SUBSCRIPTION_PRICE_DELETION);
                }

                // 3. Safe to delete
                priceRepository.delete(price);
        }

        @Override
        public BillingPricesResponse getAllActivePrices(Long productId) {
                var prices =  priceRepository.findByProductIdAndActiveTrue(productId);

                return new BillingPricesResponse(prices.stream()
                                .map(this::mapPrice)
                                .collect(Collectors.toList()));
        }

        private BillingProductDetailsDto mapProduct(BillingProduct product) {
                return BillingProductDetailsDto.builder()
                                .id(product.getId())
                                .code(product.getCode())
                                .displayName(product.getDisplayName())
                                .tier(product.getTier())
                                .monthlyTokenLimit(product.getMonthlyTokenLimit())
                                .active(product.isActive())
                                .build();
        }

        private BillingPriceDetailsDto mapPrice(BillingPrice price) {
                return BillingPriceDetailsDto.builder()
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
