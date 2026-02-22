package com.ayushsingh.doc_helper.features.user_activity.dto;

import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;

public record ActivityTarget(
        ActivityTargetType type,
        Long id
) {}
