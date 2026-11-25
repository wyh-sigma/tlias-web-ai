package com.wyh.loadtest;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 压测故障开关：只在线 loadtest profile 生效。
 *
 * <p>为什么不用"消息里带标记"：RAG 的查询改写会把用户消息重写成检索用的问句，
 * 标记会在到达模型前被丢掉。用独立开关更确定，也让 k6 场景之间互不干扰。
 *
 * <p>取值：{@code none} | {@code 429} | {@code timeout} | {@code abort}
 */
@Component
@Profile("loadtest")
public class LoadtestFaultState {

    public enum Fault {
        NONE, RATE_LIMIT, TIMEOUT, ABORT
    }

    private final AtomicReference<Fault> current = new AtomicReference<>(Fault.NONE);

    public Fault get() {
        return current.get();
    }

    public void set(String mode) {
        current.set(parse(mode));
    }

    public void clear() {
        current.set(Fault.NONE);
    }

    private Fault parse(String mode) {
        if (mode == null) {
            return Fault.NONE;
        }
        return switch (mode.toLowerCase()) {
            case "429", "rate_limit", "ratelimit" -> Fault.RATE_LIMIT;
            case "timeout" -> Fault.TIMEOUT;
            case "abort", "break" -> Fault.ABORT;
            default -> Fault.NONE;
        };
    }
}
