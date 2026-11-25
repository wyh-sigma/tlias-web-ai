package com.wyh.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具：密钥和有效期来自配置，避免硬编码在代码中。
 */
@Slf4j
@Component
public class JwtUtils {

    private static final String DEV_SECRET = "itheima-wyh-secret-key-please-change-me-32bytes!";

    private final Key secretKey;
    private final long expirationMillis;

    public JwtUtils(@Value("${tlias.jwt.secret:}") String secret,
                    @Value("${tlias.jwt.expiration:43200000}") long expirationMillis) {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("tlias.jwt.secret 未配置或长度不足 32 字节，请通过 JWT_SECRET 环境变量设置");
        }
        if (DEV_SECRET.equals(secret)) {
            log.warn("当前使用开发默认 JWT 密钥，生产环境请通过 JWT_SECRET 环境变量覆盖");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMillis = expirationMillis;
    }

    /**
     * 生成 JWT 令牌
     *
     * @param claims 载荷 (存储自定义信息，如用户id、用户名等)
     */
    public String generateJwt(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                // jti 用于退出登录后把单个令牌加入黑名单
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .compact();
    }

    /**
     * 解析 JWT 令牌
     *
     * @return 解析后的载荷
     */
    public Claims parseJwt(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }
}
