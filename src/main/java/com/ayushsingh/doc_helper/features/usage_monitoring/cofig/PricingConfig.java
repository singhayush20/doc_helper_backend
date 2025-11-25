package com.ayushsingh.doc_helper.features.usage_monitoring.cofig;

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

    /**
     * Get input cost for a model, returns default if not found
     */
    public BigDecimal getInputCost(String modelName) {
        if (modelName == null) {
            return getDefaultInputCost();
        }

        ModelPricing pricing = models.get(modelName);
        if (pricing != null && pricing.getInputCostPer1k() != null) {
            return pricing.getInputCostPer1k();
        }

        return getDefaultInputCost();
    }

    /**
     * Get output cost for a model, returns default if not found
     */
    public BigDecimal getOutputCost(String modelName) {
        if (modelName == null) {
            return getDefaultOutputCost();
        }

        ModelPricing pricing = models.get(modelName);
        if (pricing != null && pricing.getOutputCostPer1k() != null) {
            return pricing.getOutputCostPer1k();
        }

        return getDefaultOutputCost();
    }

    /**
     * Get default input cost
     */
    private BigDecimal getDefaultInputCost() {
        ModelPricing defaultPricing = models.get("default");
        return defaultPricing != null && defaultPricing.getInputCostPer1k() != null
                ? defaultPricing.getInputCostPer1k()
                : BigDecimal.valueOf(0.01);
    }

    /**
     * Get default output cost
     */
    private BigDecimal getDefaultOutputCost() {
        ModelPricing defaultPricing = models.get("default");
        return defaultPricing != null && defaultPricing.getOutputCostPer1k() != null
                ? defaultPricing.getOutputCostPer1k()
                : BigDecimal.valueOf(0.02);
    }
}
