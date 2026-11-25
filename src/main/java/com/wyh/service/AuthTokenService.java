package com.wyh.service;

import com.wyh.pojo.Emp;
import com.wyh.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 令牌管理：
 * 1. access token 用 JWT（短有效期），退出登录后按 jti 写入 Redis 黑名单；
 * 2. refresh token 用随机串存 Redis（长有效期），刷新时一次性轮换，降低泄露风险。
 */
@Slf4j
@Service
public class AuthTokenService {

    private static final String REFRESH_PREFIX = "tlias:auth:refresh:";
    private static final String BLACKLIST_PREFIX = "tlias:auth:blacklist:";
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    public AuthTokenService(JwtUtils jwtUtils, StringRedisTemplate redisTemplate) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 登录/刷新成功后签发一对令牌
     */
    public TokenPair issue(Emp emp) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", emp.getId().longValue());
        claims.put("username", emp.getUsername());
        claims.put("name", emp.getName());
        claims.put("role", emp.getRole() == null ? "USER" : emp.getRole());

        String accessToken = jwtUtils.generateJwt(claims);
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(REFRESH_PREFIX + refreshToken,
                String.valueOf(emp.getId()), REFRESH_TTL);
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * 根据 refresh token 取用户 id，不存在说明过期或已使用
     */
    public Integer resolveRefresh(String refreshToken) {
        String value = redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshToken);
        return value == null ? null : Integer.valueOf(value);
    }

    public void revokeRefresh(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(REFRESH_PREFIX + refreshToken);
        }
    }

    /**
     * 退出登录：access token 剩余有效期内拉黑，避免被盗用的令牌继续访问
     */
    public void blacklistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtUtils.parseJwt(accessToken);
            String jti = claims.getId();
            Date expiration = claims.getExpiration();
            if (jti == null || expiration == null) {
                return;
            }
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", Duration.ofMillis(ttl));
            }
        } catch (Exception e) {
            // 令牌本身无效时无需拉黑
            log.debug("退出登录时解析 access token 失败：{}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }
}
