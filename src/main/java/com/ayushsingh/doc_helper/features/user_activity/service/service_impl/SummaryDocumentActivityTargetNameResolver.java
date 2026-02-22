package com.ayushsingh.doc_helper.features.user_activity.service.service_impl;

import com.ayushsingh.doc_helper.features.doc_summary.repository.DocumentRepository;
import com.ayushsingh.doc_helper.features.doc_summary.repository.projections.DocumentNameProjection;
import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;
import com.ayushsingh.doc_helper.features.user_activity.service.ActivityTargetNameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SummaryDocumentActivityTargetNameResolver implements ActivityTargetNameResolver {

    private final DocumentRepository documentRepository;

    @Override
    public ActivityTargetType supports() {
        return ActivityTargetType.SUMMARY_DOCUMENT;
    }

    @Override
    public Map<Long, String> resolveNames(Long userId, Collection<Long> targetIds) {
        return documentRepository.findAllNameByUserIdAndIdIn(userId, targetIds)
                .stream()
                .collect(Collectors.toMap(DocumentNameProjection::id, DocumentNameProjection::fileName));
    }
}
