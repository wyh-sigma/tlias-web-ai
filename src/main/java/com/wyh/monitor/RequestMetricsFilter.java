package com.wyh.monitor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求级指标采集：统计每个接口的调用次数与耗时，用于“接口耗时 TOP10 / 功能使用频次排行”。
 * 排除 actuator 与 monitor 自身，避免指标接口污染统计。
 *
 * <p>耗时同时写入 Micrometer Timer（含 p95/p99 直方图）与按状态码的总数统计，
 * 这样 Prometheus 里能直接算错误率：sum(rate(tlias_http_responses_total{status=~"5.."}[5m]))
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestMetricsFilter extends OncePerRequestFilter {

    private final MetricsRegistry metricsRegistry;

    public RequestMetricsFilter(MetricsRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            String uri = request.getRequestURI();
            if (shouldRecord(uri)) {
                metricsRegistry.recordRequest(normalize(uri), (System.nanoTime() - start) / 1_000_000,
                        response.getStatus());
            }
        }
    }

    private boolean shouldRecord(String uri) {
        return !uri.startsWith("/actuator") && !uri.startsWith("/monitor") && !uri.contains(".");
    }

    /** 把 /students/12 归一化为 /students/{id}，避免路径参数把统计打散 */
    private String normalize(String uri) {
        String normalized = uri.replaceAll("/\\d+", "/{id}");
        return normalized.isEmpty() ? "/" : normalized;
    }
}
