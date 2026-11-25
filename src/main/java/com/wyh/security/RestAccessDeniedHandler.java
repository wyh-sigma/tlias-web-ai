package com.wyh.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyh.exception.ErrorCode;
import com.wyh.pojo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 已登录但角色不足时返回统一 JSON（403 + 4003）。
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                Result.error(ErrorCode.NO_PERMISSION.getCode(), ErrorCode.NO_PERMISSION.getMessage())));
    }
}
