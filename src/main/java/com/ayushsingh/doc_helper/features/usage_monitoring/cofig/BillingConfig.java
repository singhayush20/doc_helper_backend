package com.ayushsingh.doc_helper.features.usage_monitoring.cofig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "monetization.billing")
@Getter
@Setter
public class BillingConfig {
    private Long defaultMonthlyLimit;
    private String billingTimezone;
    private String currency;
}