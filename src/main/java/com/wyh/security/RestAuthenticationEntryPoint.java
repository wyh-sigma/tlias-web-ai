package com.wyh.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyh.exception.ErrorCode;
import com.wyh.pojo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未登录/令牌失效时返回统一 JSON，而不是 Spring Security 默认的登录页跳转。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                Result.error(ErrorCode.NOT_LOGIN.getCode(), ErrorCode.NOT_LOGIN.getMessage())));
    }
}
