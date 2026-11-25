package com.wyh.loadtest;

import com.wyh.pojo.Result;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 压测故障注入入口，仅 loadtest profile 存在。
 *
 * <pre>
 *   POST /loadtest/fault?mode=429|timeout|abort|none
 * </pre>
 *
 * 调用后，假模型对后续请求按该模式失败，用于验证兜底、重试与错误指标。
 */
@RestController
@RequestMapping("/loadtest")
@Profile("loadtest")
public class LoadtestFaultController {

    private final LoadtestFaultState faultState;

    public LoadtestFaultController(LoadtestFaultState faultState) {
        this.faultState = faultState;
    }

    @PostMapping("/fault")
    public Result fault(@RequestParam(defaultValue = "none") String mode) {
        faultState.set(mode);
        return Result.success(java.util.Map.of("fault", faultState.get().name()));
    }

    @PostMapping("/fault/clear")
    public Result clear() {
        faultState.clear();
        return Result.success(java.util.Map.of("fault", faultState.get().name()));
    }
}
