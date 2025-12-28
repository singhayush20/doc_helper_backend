package com.ayushsingh.doc_helper.features.user_plan.projection;

import java.time.Instant;

import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;
import com.ayushsingh.doc_helper.features.user_plan.entity.SubscriptionStatus;

public interface CurrentSubscriptionView {

    String getPlanCode();

    String getPriceCode();

    SubscriptionStatus getStatus();

    Boolean getCancelAtPeriodEnd();

    Instant getCurrentPeriodStart();

    Instant getCurrentPeriodEnd();

    String getPlanName();

    AccountTier getPlanTier();

    Long getPlanMonthlyTokenLimit();

    Long getAmount();

    String getCurrency();

    String getPriceDescription();
}
