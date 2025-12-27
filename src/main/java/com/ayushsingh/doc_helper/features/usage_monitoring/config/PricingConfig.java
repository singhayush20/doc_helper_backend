package com.ayushsingh.doc_helper.features.usage_monitoring.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "monetization.model-costs")
@Getter
@Setter
public class PricingConfig {

    private Map<String, ModelPricing> models = new HashMap<>();

    @Getter
    @Setter
    public static class ModelPricing {
        private BigDecimal inputCostPer1k;
        private BigDecimal outputCostPer1k;
    }

    public BigDecimal getInputCost(String modelName) {
        return resolvePricing(modelName).getInputCostPer1k();
    }

    public BigDecimal getOutputCost(String modelName) {
        return resolvePricing(modelName).getOutputCostPer1k();
    }

    private ModelPricing resolvePricing(String modelName) {
        String key = (modelName != null) ? modelName : "default";
        ModelPricing pricing = models.getOrDefault(key, models.get("default"));
        if (pricing == null) {
            throw new IllegalStateException("No pricing configured for model '" + key + "' and no 'default' pricing");
        }
        return pricing;
    }
}
