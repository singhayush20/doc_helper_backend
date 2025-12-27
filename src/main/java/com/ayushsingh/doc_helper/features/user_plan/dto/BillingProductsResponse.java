package com.ayushsingh.doc_helper.features.user_plan.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BillingProductsResponse {
    public List<BillingProductDetailsDto> products;
}
