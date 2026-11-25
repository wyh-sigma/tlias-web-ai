package com.wyh.controller;


import com.wyh.anno.Log;
import com.wyh.dto.LoginDTO;
import com.wyh.dto.TokenDTO;
import com.wyh.pojo.Result;
import com.wyh.security.JwtAuthenticationFilter;
import com.wyh.service.EmpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping
public class LoginController {

    @Autowired
    private EmpService empService;

    @PostMapping("/login")
    @Log
    public Result login(@Valid @RequestBody LoginDTO loginDTO) {
        // 只记录用户名，禁止把密码写进日志
        log.info("登录请求, username={}", loginDTO.getUsername());
        return Result.success(empService.login(loginDTO));
    }

    /**
     * 用 refresh token 换新的一对令牌（refresh token 一次一换）
     */
    @PostMapping("/refresh")
    public Result refresh(@RequestBody TokenDTO tokenDTO) {
        return Result.success(empService.refresh(tokenDTO.getRefreshToken()));
    }

    /**
     * 退出登录：access token 加入黑名单，同时作废 refresh token
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request,
                         @RequestBody(required = false) TokenDTO tokenDTO) {
        String accessToken = JwtAuthenticationFilter.resolveToken(request);
        String refreshToken = tokenDTO == null ? null : tokenDTO.getRefreshToken();
        empService.logout(accessToken, refreshToken);
        return Result.success();
    }
}
