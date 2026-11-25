package com.wyh.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败锁定：同一用户名 15 分钟内连续失败 5 次后锁定 15 分钟。
 * 计数放在 Redis，多实例部署时也能生效。
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final String PREFIX = "tlias:auth:fail:";

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLocked(String username) {
        String value = redisTemplate.opsForValue().get(PREFIX + username);
        return value != null && Integer.parseInt(value) >= MAX_FAILURES;
    }

    public void onFailure(String username) {
        String key = PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW);
        }
    }

    public void onSuccess(String username) {
        redisTemplate.delete(PREFIX + username);
    }
}
