package com.ayushsingh.doc_helper.features.user_activity.service.service_impl;

import com.ayushsingh.doc_helper.core.caching.RedisKeys;
import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityDto;
import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityResponseDto;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivity;
import com.ayushsingh.doc_helper.features.user_activity.repository.UserActivityRepository;
import com.ayushsingh.doc_helper.features.user_activity.service.UserActivityService;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocNameProjection;
import com.ayushsingh.doc_helper.features.user_doc.service.UserDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final UserDocService userDocService;

    @Override
    public UserActivityResponseDto fetchUserActivity(int limit) {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        String key = RedisKeys.recentDocsKey(userId);

        // Redis Sorted Set ensures ordered list access
        // Without this, we would need to have a custom sorting logic
        Set<String> rawDocIds =
                redis.opsForZSet()
                        .reverseRange(key, 0, limit - 1);

        // If no docs in Redis, fallback to DB
        if (rawDocIds == null || rawDocIds.isEmpty()) {
            List<UserActivity> activities =
                    userActivityRepository
                            .findTop5ByUserIdOrderByDominantAtDesc(userId);

            List<Long> documentIds =
                    activities.stream()
                            .map(UserActivity::getDocumentId)
                            .toList();

            Map<Long, String> docIdToName =
                    userDocService.findAllDocNamesByIdIn(documentIds)
                            .stream()
                            .collect(Collectors.toMap(
                                    UserDocNameProjection::id,
                                    UserDocNameProjection::fileName
                            ));

            return new UserActivityResponseDto(
                    activities.stream()
                            .map(activity ->
                                    userActivityToDto(
                                            activity,
                                            docIdToName.get(activity.getDocumentId())
                                    )
                            )
                            .toList()
            );
        }

        List<Long> documentIds =
                rawDocIds.stream()
                        .map(Long::valueOf)
                        .toList();

        List<UserActivity> activities =
                userActivityRepository
                        .findAllByUserIdAndDocumentIdIn(userId, documentIds);

        // Build map for quick lookup, since the order is not guaranteed by the db fetch query
        Map<Long, UserActivity> byDocId =
                activities.stream()
                        .collect(Collectors.toMap(
                                UserActivity::getDocumentId,
                                Function.identity()
                        ));

        Map<Long, String> docIdToName =
                userDocService.findAllDocNamesByIdIn(documentIds)
                        .stream()
                        .collect(Collectors.toMap(
                                UserDocNameProjection::id,
                                UserDocNameProjection::fileName
                        ));


        // Rebuild list in Redis order
        List<UserActivityDto> result = new ArrayList<>();

        for (Long docId : documentIds) {
            UserActivity activity = byDocId.get(docId);
            String fileName = docIdToName.get(docId);
            if (activity != null) {
                result.add(userActivityToDto(activity,fileName));
            }
        }

        return new UserActivityResponseDto(result);
    }

    private UserActivityDto userActivityToDto(UserActivity userActivity, String fileName) {
        return UserActivityDto.builder()
                .documentId(userActivity.getDocumentId())
                .dominantAt(userActivity.getDominantAt())
                .lastAction(userActivity.getLastAction())
                .fileName(fileName)
                .dominantActivity(userActivity.getDominantActivity())
                .build();
    }
}
