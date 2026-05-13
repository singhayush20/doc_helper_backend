package com.ayushsingh.doc_helper.features.feature_workflow.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisDistributedLockServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisDistributedLockService redisDistributedLockService;

    @Test
    void acquireLockShouldReturnFalseWhenAnotherNodeAlreadyOwnsTheLock() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("workflow_execution:1"), anyString(), eq(Duration.ofMillis(5000))))
                .thenReturn(false);

        boolean acquired = redisDistributedLockService.acquireLock("workflow_execution:1", 5000L);

        assertFalse(acquired);
    }

    @Test
    void acquireLockShouldDegradeToDatabaseProtectionWhenRedisIsUnavailable() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("workflow_execution:1"), anyString(), eq(Duration.ofMillis(5000))))
                .thenThrow(new RuntimeException("redis down"));

        boolean acquired = redisDistributedLockService.acquireLock("workflow_execution:1", 5000L);

        assertTrue(acquired);
    }

    @Test
    void releaseLockShouldOnlyDeleteWhenCurrentThreadOwnsTheLock() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("workflow_execution:1"), anyString(), eq(Duration.ofMillis(5000))))
                .thenReturn(true);

        assertTrue(redisDistributedLockService.acquireLock("workflow_execution:1", 5000L));

        redisDistributedLockService.releaseLock("workflow_execution:1");
        redisDistributedLockService.releaseLock("workflow_execution:1");

        verify(stringRedisTemplate).execute(
                any(RedisScript.class),
                eq(Collections.singletonList("workflow_execution:1")),
                anyString());
        verify(stringRedisTemplate, never()).execute(
                any(RedisScript.class),
                eq(Collections.singletonList("workflow_execution:missing")),
                anyString());
    }
}
