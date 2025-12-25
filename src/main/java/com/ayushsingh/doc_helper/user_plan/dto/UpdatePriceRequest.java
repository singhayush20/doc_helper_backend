package com.ayushsingh.doc_helper.user_plan.dto;

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
    private String currency;
    private Integer intervalMonths;
    private Long tokenLimit;
}