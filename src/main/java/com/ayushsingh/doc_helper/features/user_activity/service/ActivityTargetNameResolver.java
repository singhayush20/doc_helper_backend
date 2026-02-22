package com.ayushsingh.doc_helper.features.user_activity.service;

import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;

import java.util.Collection;
import java.util.Map;

public interface ActivityTargetNameResolver {

    ActivityTargetType supports();

    Map<Long, String> resolveNames(Long userId, Collection<Long> targetIds);
}
