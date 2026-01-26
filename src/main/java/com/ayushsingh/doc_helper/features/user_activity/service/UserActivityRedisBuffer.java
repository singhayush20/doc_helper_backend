    package com.ayushsingh.doc_helper.features.user_activity.service;

    import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityWriteRequest;
    import com.fasterxml.jackson.core.JsonProcessingException;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.stereotype.Service;

    import java.time.Duration;
    import java.util.Map;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class UserActivityRedisBuffer {

        private static final String BUFFER_KEY = "activity:buffer";

        private final RedisTemplate<String, String> redis;
        private final ObjectMapper objectMapper;

        public void buffer(UserActivityWriteRequest writeRequest) {
            String field = writeRequest.userId() + ":" + writeRequest.documentId();

            try {
                String value = objectMapper.writeValueAsString(writeRequest);
                redis.opsForHash().put(BUFFER_KEY, field, value);
                redis.expire(BUFFER_KEY, Duration.ofMinutes(5));
            } catch (JsonProcessingException e) {
                // fail open
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

