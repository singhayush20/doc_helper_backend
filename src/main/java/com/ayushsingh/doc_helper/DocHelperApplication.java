package com.ayushsingh.doc_helper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ayushsingh.doc_helper.features.payments.config.PaymentGatewayConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.config.BillingConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.config.PlanConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.config.PricingConfig;


@SpringBootApplication
@EnableCaching
@EnableMongoAuditing
@EnableScheduling
@EnableConfigurationProperties({ BillingConfig.class, PricingConfig.class, PlanConfig.class, PaymentGatewayConfig.class })
public class DocHelperApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocHelperApplication.class, args);
	}
}
