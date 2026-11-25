package com.wyh.config;

import com.wyh.security.JwtAuthenticationFilter;
import com.wyh.security.RestAccessDeniedHandler;
import com.wyh.security.RestAuthenticationEntryPoint;
import com.wyh.service.AuthTokenService;
import com.wyh.utils.JwtUtils;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;

/**
 * Spring Security 过滤链：无状态 JWT 认证 + 基于角色的接口权限。
 * 读写权限划分：ADMIN 可增删改查；USER 只能查询业务数据和使用 AI 助手。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtUtils jwtUtils,
                                                   AuthTokenService authTokenService,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtils, authTokenService);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 不使用 Spring Security 自带的登出，登出逻辑在 /logout 控制器中处理
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // SSE 流式响应（AI 对话）会以 ASYNC 分发再次进入过滤链，
                        // 若不在这里放行，已提交的响应会被二次鉴权命中 AccessDenied，导致 HTTP 500
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login", "/refresh").permitAll()
                        // 健康检查与 Prometheus 采集给运维/监控系统使用
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 管理端写操作
                        .requestMatchers(HttpMethod.POST, "/emps", "/depts", "/clazzs", "/students", "/upload")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/emps", "/depts", "/clazzs", "/students", "/students/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/emps", "/depts/**", "/clazzs/**", "/students")
                        .hasRole("ADMIN")

                        // 报表、知识库重建与运行监控属于管理能力
                        .requestMatchers("/report/**", "/ai/knowledge/**", "/monitor/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
