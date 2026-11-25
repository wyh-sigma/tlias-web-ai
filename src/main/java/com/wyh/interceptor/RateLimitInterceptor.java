package com.wyh.interceptor;

import com.wyh.exception.BusinessException;
import com.wyh.exception.ErrorCode;
import com.wyh.monitor.MetricsRegistry;
import com.wyh.utils.BaseContext;
import com.wyh.utils.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * AI 接口限流：登录拦截器执行后，按用户身份限制调用频率。
 *
 * <p>额度可通过 tlias.ai.rate-limit.* 配置；压测 profile 会把它放大，
 * 以便单独用 429 场景验证限流逻辑本身，而不是被限流挡住压不出容量。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisRateLimiter rateLimiter;
    private final MetricsRegistry metricsRegistry;
    private final int limitPerWindow;
    private final Duration window;

    public RateLimitInterceptor(RedisRateLimiter rateLimiter,
                               MetricsRegistry metricsRegistry,
                               @Value("${tlias.ai.rate-limit.per-minute:20}") int limitPerMinute,
                               @Value("${tlias.ai.rate-limit.window-seconds:60}") long windowSeconds) {
        this.rateLimiter = rateLimiter;
        this.metricsRegistry = metricsRegistry;
        this.limitPerWindow = limitPerMinute;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = BaseContext.getCurrentId();
        String identity = userId == null ? request.getRemoteAddr() : userId.toString();

        if (!rateLimiter.tryAcquire("ai", identity, limitPerWindow, window)) {
            // 记录限流触发次数，供监控页与告警使用
            metricsRegistry.recordRateLimited();
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return true;
    }
}
