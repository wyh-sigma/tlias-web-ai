package com.wyh.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的固定窗口限流：同一身份在窗口期内超过上限就拒绝。
 * 多实例部署时也能共享计数，比 JVM 内存计数更适合生产。
 */
@Component
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(String scene, String identity, int limit, Duration window) {
        long windowIndex = System.currentTimeMillis() / window.toMillis();
        String key = "tlias:rate:" + scene + ":" + identity + ":" + windowIndex;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
        return count == null || count <= limit;
    }
}
