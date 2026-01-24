package com.ayushsingh.doc_helper.features.user_plan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCancelRequest {
    private int paymentFailureErrorCode;
    private String paymentFailureErrorMessage;
}
