package com.wyh.monitor;

import com.wyh.pojo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运行监控接口：给前端监控页与压测脚本提供实时服务端指标。
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    private final MetricsRegistry metricsRegistry;

    public MonitorController(MetricsRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
    }

    /** 接口耗时 TOP10、功能使用频次排行、总请求量 */
    @GetMapping("/overview")
    public Result overview() {
        return Result.success(metricsRegistry.overview());
    }

    /** AI 指标：命中率/兜底率、平均响应、首字延迟、Token 与成本、限流次数 */
    @GetMapping("/ai")
    public Result ai() {
        return Result.success(metricsRegistry.aiSnapshot());
    }
}
