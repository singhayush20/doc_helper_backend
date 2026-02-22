package com.ayushsingh.doc_helper.features.user_activity.service.service_impl;

import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;
import com.ayushsingh.doc_helper.features.user_activity.service.ActivityTargetNameResolver;
import com.ayushsingh.doc_helper.features.user_doc.repository.UserDocRepository;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocNameProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserDocActivityTargetNameResolver implements ActivityTargetNameResolver {

    private final UserDocRepository userDocRepository;

    @Override
    public ActivityTargetType supports() {
        return ActivityTargetType.USER_DOC;
    }

    @Override
    public Map<Long, String> resolveNames(Long userId, Collection<Long> targetIds) {
        return userDocRepository.findAllByUserIdAndIdIn(userId, targetIds)
                .stream()
                .collect(Collectors.toMap(UserDocNameProjection::id, UserDocNameProjection::fileName));
    }
}
