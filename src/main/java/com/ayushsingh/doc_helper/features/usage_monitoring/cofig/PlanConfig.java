package com.ayushsingh.doc_helper.features.usage_monitoring.cofig;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ayushsingh.doc_helper.features.usage_monitoring.entity.AccountTier;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "monetization.plans")
@Getter
@Setter
public class PlanConfig {
    private Map<AccountTier, PlanLimits> tiers = new HashMap<>();

    @Getter
    @Setter
    public static class PlanLimits {
        private Long monthlyTokenLimit;
        private Integer maxDocuments;
    }

    public PlanLimits getLimits(AccountTier tier) {
        return tiers.getOrDefault(tier, tiers.get(AccountTier.FREE));
    }
}
