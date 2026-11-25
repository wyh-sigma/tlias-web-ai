package com.wyh.security;

import com.wyh.service.AuthTokenService;
import com.wyh.utils.BaseContext;
import com.wyh.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：校验签名、黑名单后把用户写入 SecurityContext。
 * 解析失败不直接返回错误，交给 Security 的入口点统一返回 401。
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final AuthTokenService authTokenService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, AuthTokenService authTokenService) {
        this.jwtUtils = jwtUtils;
        this.authTokenService = authTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            authenticate(token);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 线程池复用，必须清理，避免身份串到下一个请求
            BaseContext.removeCurrentId();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtUtils.parseJwt(token);
            if (authTokenService.isBlacklisted(claims.getId())) {
                return;
            }
            Long userId = claims.get("id", Long.class);
            String username = claims.get("username", String.class);
            String name = claims.get("name", String.class);
            String role = claims.get("role", String.class);
            role = (role == null || role.isBlank()) ? "USER" : role;

            AuthUser principal = new AuthUser(userId, username, name, role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            BaseContext.setCurrentId(userId);
        } catch (Exception e) {
            log.debug("JWT 校验失败：{}", e.getMessage());
        }
    }

    /**
     * 只接受标准 Authorization: Bearer &lt;token&gt;，不再兼容自定义 token 请求头
     */
    public static String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HEADER_AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
