package com.ayushsingh.doc_helper.features.user_plan.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillingPricesResponse {
    public List<BillingPriceDetailsDto> prices;
}
