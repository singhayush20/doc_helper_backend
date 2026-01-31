package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.features.product_features.entity.UsageQuota;
import com.ayushsingh.doc_helper.features.product_features.repository.UsageQuotaRepository;
import com.ayushsingh.doc_helper.features.product_features.service.UsageQuotaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsageQuotaServiceImpl implements UsageQuotaService {

    private final UsageQuotaRepository quotaRepo;

    @Transactional
    public void consume(
            Long userId,
            String featureCode,
            String metric,
            long amount
    ) {
        UsageQuota quota = quotaRepo
                .findByUserIdAndFeatureCodeAndMetric(userId, featureCode, metric)
                .orElseThrow(() -> new ForbiddenException("Quota not configured"));

        if (quota.getUsed() + amount > quota.getLimit()) {
            throw new ForbiddenException("Quota exceeded");
        }

        quota.setUsed(quota.getUsed() + amount);
        quotaRepo.save(quota);
    }
}
