    package com.ayushsingh.doc_helper.features.user_activity.service;

    import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityWriteRequest;
    import com.fasterxml.jackson.core.JsonProcessingException;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.stereotype.Service;

    import java.time.Duration;
    import java.util.Map;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class UserActivityRedisBuffer {

        private static final String BUFFER_KEY = "activity:buffer";

        private final RedisTemplate<String, String> redis;
        private final ObjectMapper objectMapper;

        public void buffer(UserActivityWriteRequest writeRequest) {
            String field = writeRequest.userId() + ":" + writeRequest.targetType() + ":" + writeRequest.targetId();

            try {
                String value = objectMapper.writeValueAsString(writeRequest);
                redis.opsForHash().put(BUFFER_KEY, field, value);
                // Set the buffer to expire after 5 minutes to prevent memory leaks
                // and to prevent the buffer from growing indefinitely, in case of a
                // failure to manually clean the buffer entries
                redis.expire(BUFFER_KEY, Duration.ofMinutes(5));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize user activity write request: {}",writeRequest, e);
            }
        }

        public Map<String, UserActivityWriteRequest> drain() {
            Map<Object, Object> entries =
                    redis.opsForHash().entries(BUFFER_KEY);

            if (entries.isEmpty()) {
                return Map.of();
            }

            redis.delete(BUFFER_KEY);

            return entries.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> deserialize(e.getValue().toString())
                    ));
        }

        private UserActivityWriteRequest deserialize(String json) {
            try {
                return objectMapper.readValue(
                        json,
                        UserActivityWriteRequest.class
                );
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

