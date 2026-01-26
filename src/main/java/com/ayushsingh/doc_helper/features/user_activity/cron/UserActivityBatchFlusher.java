package com.ayushsingh.doc_helper.features.user_activity.cron;

import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityWriteRequest;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivity;
import com.ayushsingh.doc_helper.features.user_activity.repository.UserActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActivityBatchFlusher {

    private static final String BUFFER_KEY = "activity:buffer";
    private static final String LOCK_KEY = "activity:flush:lock";

    private static final Duration LOCK_TTL = Duration.ofSeconds(20);

    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;
    private final UserActivityRepository repository;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void flush() {

        Boolean locked = redis.opsForValue()
                .setIfAbsent(LOCK_KEY, "1", LOCK_TTL);

        if (!Boolean.TRUE.equals(locked)) {
            return; // another instance is flushing
        }

        try {
            Map<Object, Object> entries =
                    redis.opsForHash().entries(BUFFER_KEY);

            if (entries.isEmpty()) {
                return;
            }

            List<UserActivity> activities = new ArrayList<>(entries.size());

            for (Object value : entries.values()) {
                UserActivityWriteRequest cmd =
                        deserialize(value.toString());

                activities.add(toEntity(cmd));
            }

            repository.saveAll(activities);

            redis.delete(BUFFER_KEY);

            log.debug("Flushed {} user activities", activities.size());

        } catch (Exception e) {
            log.error("Activity flush failed, will retry", e);
        }
    }


    private UserActivityWriteRequest deserialize(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    UserActivityWriteRequest.class
            );
        } catch (Exception e) {
            throw new IllegalStateException("Invalid activity payload", e);
        }
    }

    private UserActivity toEntity(UserActivityWriteRequest cmd) {
        UserActivity ua = new UserActivity();
        ua.setUserId(cmd.userId());
        ua.setDocumentId(cmd.documentId());
        ua.setDominantActivity(cmd.dominantActivity());
        ua.setLastAction(cmd.lastAction());
        ua.setDominantAt(cmd.dominantAt());
        ua.setLastActionAt(cmd.lastActionAt());
        ua.setMetadata(cmd.metadata());
        return ua;
    }
}
