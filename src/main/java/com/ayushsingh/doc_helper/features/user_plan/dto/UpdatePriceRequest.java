package com.ayushsingh.doc_helper.features.user_plan.dto;


import com.ayushsingh.doc_helper.features.user_plan.entity.Currency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePriceRequest {
    private Long amount;
    private Currency currency;
    private Integer intervalMonths;
}