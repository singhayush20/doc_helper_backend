package com.ayushsingh.doc_helper.features.user_activity.service;

import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityResponseDto;

public interface UserActivityService {
    UserActivityResponseDto fetchUserActivity(int limit);
}
