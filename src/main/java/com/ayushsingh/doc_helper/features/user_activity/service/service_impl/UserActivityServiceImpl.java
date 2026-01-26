package com.ayushsingh.doc_helper.features.user_activity.service.service_impl;

import com.ayushsingh.doc_helper.core.caching.RedisKeys;
import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityDto;
import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityResponseDto;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivity;
import com.ayushsingh.doc_helper.features.user_activity.repository.UserActivityRepository;
import com.ayushsingh.doc_helper.features.user_activity.service.UserActivityService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserActivityServiceImpl implements UserActivityService {

    private final RedisTemplate<String, String> redis;
    private final UserActivityRepository userActivityRepository;

    public UserActivityServiceImpl(
            RedisTemplate<String, String> redis,
            UserActivityRepository userActivityRepository
    ) {
        this.redis = redis;
        this.userActivityRepository = userActivityRepository;
    }

    @Override
    public UserActivityResponseDto fetchUserActivity(int limit) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        String key = RedisKeys.recentDocsKey(userId);

        Set<String> rawDocIds =
                redis.opsForZSet()
                        .reverseRange(key, 0, limit - 1);

        if (rawDocIds == null || rawDocIds.isEmpty()) {
            return new UserActivityResponseDto(userActivityRepository
                    .findTop5ByUserIdOrderByDominantAtDesc(userId)
                    .stream()
                    .map(this::userActivityToDto)
                    .toList());
        }

        List<Long> documentIds =
                rawDocIds.stream()
                        .map(Long::valueOf)
                        .toList();

        List<UserActivity> activities =
                userActivityRepository
                        .findAllByUserIdAndDocumentIdIn(userId, documentIds);

        Map<Long, UserActivity> byDocId =
                activities.stream()
                        .collect(Collectors.toMap(
                                UserActivity::getDocumentId,
                                Function.identity()
                        ));

        // Rebuild list in Redis order
        List<UserActivityDto> result = new ArrayList<>();

        for (Long docId : documentIds) {
            UserActivity activity = byDocId.get(docId);
            if (activity != null) {
                result.add(userActivityToDto(activity));
            }
        }

        return new UserActivityResponseDto(result);
    }

    private UserActivityDto userActivityToDto(UserActivity userActivity) {
        return UserActivityDto.builder()
                .documentId(userActivity.getDocumentId())
                .dominantAt(userActivity.getDominantAt())
                .lastAction(userActivity.getLastAction())
                .dominantActivity(userActivity.getDominantActivity())
                .build();
    }
}
