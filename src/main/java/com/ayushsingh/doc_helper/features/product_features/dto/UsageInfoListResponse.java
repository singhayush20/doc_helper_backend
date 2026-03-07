package com.ayushsingh.doc_helper.features.product_features.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsageInfoListResponse {
    
    private List<UsageInfoResponse> usageInfo;
}
