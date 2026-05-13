package com.ayushsingh.doc_helper.features.feature_workflow.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisDistributedLockService implements DistributedLockService {

    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    end
                    return 0
                    """,
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    private final ThreadLocal<Map<String, String>> ownedLockTokens =
            ThreadLocal.withInitial(HashMap::new);

    @Override
    public boolean acquireLock(String key, long timeoutMillis) {
        String token = UUID.randomUUID().toString();

        // TODO: Handle lock for long running operations (SSE+Async step
        //  execution or Sync+Lock Renewal)
        //  https://chatgpt.com/g/g-p-697511b4c5988191b24154312820bf8b-doc-helper/c/69cc04f4-db2c-8322-beae-11da0ed818f7
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, token, Duration.ofMillis(timeoutMillis));

            if (Boolean.TRUE.equals(acquired)) {
                ownedLockTokens.get().put(key, token);
                return true;
            }

            return false;
        } catch (RuntimeException ex) {
            log.warn("Redis lock unavailable for key {}. Proceeding with DB-only protection.", key, ex);
            return true;
        }
    }

    @Override
    public void releaseLock(String key) {
        Map<String, String> tokens = ownedLockTokens.get();
        String token = tokens.remove(key);

        try {
            if (token != null) {
                stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(key), token);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to release Redis lock for key {}", key, ex);
        } finally {
            if (tokens.isEmpty()) {
                ownedLockTokens.remove();
            }
        }
    }
}
