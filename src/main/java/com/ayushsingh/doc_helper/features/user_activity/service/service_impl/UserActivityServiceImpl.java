package com.ayushsingh.doc_helper.features.user_activity.service.service_impl;

import com.ayushsingh.doc_helper.core.caching.RedisKeys;
import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.user_activity.dto.ActivityRedisMember;
import com.ayushsingh.doc_helper.features.user_activity.dto.ActivityTarget;
import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityDto;
import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityResponseDto;
import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivity;
import com.ayushsingh.doc_helper.features.user_activity.repository.UserActivityRepository;
import com.ayushsingh.doc_helper.features.user_activity.service.ActivityTargetNameResolver;
import com.ayushsingh.doc_helper.features.user_activity.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserActivityServiceImpl implements UserActivityService {

    private final RedisTemplate<String, String> redis;
    private final UserActivityRepository userActivityRepository;
    private final List<ActivityTargetNameResolver> nameResolvers;

    @Override
    public UserActivityResponseDto fetchUserActivity(int limit) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        String key = RedisKeys.recentDocsKey(userId);

        Set<String> rawTargetMembers =
                redis.opsForZSet()
                        .reverseRange(key, 0, limit - 1);

        if (rawTargetMembers == null || rawTargetMembers.isEmpty()) {
            List<UserActivity> activities =
                    userActivityRepository.findTop5ByUserIdOrderByDominantAtDesc(userId);
            return new UserActivityResponseDto(toDtos(userId, activities));
        }

        List<ActivityTarget> orderedTargets = rawTargetMembers.stream()
                .map(ActivityRedisMember::parse)
                .filter(java.util.Objects::nonNull)
                .toList();

        List<UserActivity> activities = findActivitiesInRedisOrder(userId, orderedTargets);
        return new UserActivityResponseDto(toDtos(userId, activities, orderedTargets));
    }

    private List<UserActivity> findActivitiesInRedisOrder(Long userId, List<ActivityTarget> orderedTargets) {
        Map<ActivityTargetType, List<Long>> idsByType = orderedTargets.stream()
                .collect(Collectors.groupingBy(
                        ActivityTarget::type,
                        () -> new EnumMap<>(ActivityTargetType.class),
                        Collectors.mapping(ActivityTarget::id, Collectors.toList())
                ));

        List<UserActivity> all = new ArrayList<>();
        idsByType.forEach((type, ids) -> all.addAll(
                userActivityRepository.findAllByUserIdAndTargetTypeAndTargetIdIn(userId, type, ids)
        ));
        return all;
    }

    private List<UserActivityDto> toDtos(Long userId, List<UserActivity> activities) {
        return toDtos(userId, activities, null);
    }

    private List<UserActivityDto> toDtos(Long userId, List<UserActivity> activities, List<ActivityTarget> orderedTargets) {
        Map<ActivityTargetType, ActivityTargetNameResolver> resolverByType = nameResolvers.stream()
                .collect(Collectors.toMap(ActivityTargetNameResolver::supports, Function.identity()));

        Map<ActivityTargetType, List<Long>> idsByType = activities.stream()
                .collect(Collectors.groupingBy(
                        this::targetTypeOf,
                        () -> new EnumMap<>(ActivityTargetType.class),
                        Collectors.mapping(UserActivity::getTargetId, Collectors.toList())
                ));

        Map<ActivityTargetType, Map<Long, String>> namesByType = new EnumMap<>(ActivityTargetType.class);
        idsByType.forEach((type, ids) -> {
            ActivityTargetNameResolver resolver = resolverByType.get(type);
            if (resolver != null) {
                namesByType.put(type, resolver.resolveNames(userId, ids));
            } else {
                namesByType.put(type, Map.of());
            }
        });

        if (orderedTargets == null) {
            return activities.stream()
                    .map(activity -> userActivityToDto(activity, resolveName(activity, namesByType)))
                    .toList();
        }

        Map<String, UserActivity> byTarget = activities.stream()
                .collect(Collectors.toMap(
                        a -> targetTypeOf(a).name() + ":" + a.getTargetId(),
                        Function.identity(),
                        (a, b) -> a
                ));

        List<UserActivityDto> result = new ArrayList<>();
        for (ActivityTarget target : orderedTargets) {
            UserActivity activity = byTarget.get(target.type().name() + ":" + target.id());
            if (activity != null) {
                result.add(userActivityToDto(activity, resolveName(activity, namesByType)));
            }
        }
        return result;
    }

    private String resolveName(UserActivity activity, Map<ActivityTargetType, Map<Long, String>> namesByType) {
        return namesByType
                .getOrDefault(targetTypeOf(activity), Map.of())
                .getOrDefault(activity.getTargetId(), "Unknown resource");
    }


    private ActivityTargetType targetTypeOf(UserActivity activity) {
        return activity.getTargetType() != null ? activity.getTargetType() : ActivityTargetType.USER_DOC;
    }

    private UserActivityDto userActivityToDto(UserActivity userActivity, String fileName) {
        return UserActivityDto.builder()
                .targetType(targetTypeOf(userActivity))
                .documentId(userActivity.getTargetId())
                .dominantAt(userActivity.getDominantAt())
                .lastAction(userActivity.getLastAction())
                .fileName(fileName)
                .dominantActivity(userActivity.getDominantActivity())
                .build();
    }
}
