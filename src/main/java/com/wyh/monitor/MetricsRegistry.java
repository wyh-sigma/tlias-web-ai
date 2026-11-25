package com.wyh.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 运行指标注册表。
 *
 * <p>指标走两条路，各司其职：
 * <ul>
 *   <li>Micrometer Timer / Counter → /actuator/prometheus，给 Prometheus + Grafana 长期留存与告警</li>
 *   <li>进程内环形样本 → /monitor 接口，直接给出 p50/p95/p99，供压测当场的监控页展示</li>
 * </ul>
 *
 * <p>为什么不只用平均值：平均值会把长尾淹没。10 次请求里 9 次 5ms、1 次 2s，
 * 平均值仍然很好看，但用户已经在骂了。所有压测判据都必须是分位数。
 *
 * <p>为什么 Prometheus 那边不再自己算：Prometheus 的分位数由 histogram_quantile 在查询时算，
 * 进程内拿不到它的桶值，所以这里保留一份有上限的本地样本用于 /monitor。
 */
@Component
public class MetricsRegistry {

    /** 每个接口/模型最多保留的样本数，环形覆盖旧样本 */
    private static final int MAX_SAMPLES = 2048;

    private final MeterRegistry registry;

    /** uri -> 请求 Timer */
    private final ConcurrentHashMap<String, Timer> requestTimers = new ConcurrentHashMap<>();
    /** name|model -> AI Timer */
    private final ConcurrentHashMap<String, Timer> aiTimers = new ConcurrentHashMap<>();

    /** 进程内分位数样本 */
    private final ConcurrentHashMap<String, SampleBuffer> requestSamples = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SampleBuffer> aiSamples = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SampleBuffer> aiTtftSamples = new ConcurrentHashMap<>();

    private final LongAdder totalRequests = new LongAdder();

    private final LongAdder aiRequests = new LongAdder();
    private final LongAdder promptTokens = new LongAdder();
    private final LongAdder completionTokens = new LongAdder();
    private final AtomicInteger aiInflight = new AtomicInteger();

    /** 估算单价，单位：元/百万 token，可通过配置覆盖 */
    private final double inputPricePerMillion;
    private final double outputPricePerMillion;

    public MetricsRegistry(MeterRegistry registry,
                           @Value("${tlias.ai.cost.input-per-million:2.0}") double inputPricePerMillion,
                           @Value("${tlias.ai.cost.output-per-million:8.0}") double outputPricePerMillion) {
        this.registry = registry;
        this.inputPricePerMillion = inputPricePerMillion;
        this.outputPricePerMillion = outputPricePerMillion;
        Gauge.builder("tlias.ai.inflight", aiInflight, AtomicInteger::get)
                .description("正在处理中的 AI 对话数")
                .register(registry);
    }

    /* ==================== HTTP 请求指标 ==================== */

    public void recordRequest(String path, long costMs) {
        recordRequest(path, costMs, 200);
    }

    public void recordRequest(String path, long costMs, int status) {
        totalRequests.increment();
        requestTimers.computeIfAbsent(path, this::newRequestTimer).record(Duration.ofMillis(costMs));
        requestSamples.computeIfAbsent(path, key -> new SampleBuffer()).add(costMs);
        Counter.builder("tlias.http.responses")
                .tag("uri", path)
                .tag("status", String.valueOf(status))
                .description("按状态码统计的响应数")
                .register(registry)
                .increment();
    }

    private Timer newRequestTimer(String path) {
        return Timer.builder("tlias.http.requests")
                .tag("uri", path)
                .description("HTTP 接口耗时")
                .publishPercentileHistogram()
                .register(registry);
    }

    /** 按 p95 倒序排行，而不是按平均值 */
    public List<Map<String, Object>> slowApis(int topN) {
        List<Map<String, Object>> list = new ArrayList<>();
        requestSamples.forEach((path, samples) -> {
            if (samples.count() == 0) {
                return;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", path);
            item.put("count", samples.count());
            item.put("avgMs", round(samples.average()));
            item.put("p50Ms", round(samples.percentile(0.50)));
            item.put("p95Ms", round(samples.percentile(0.95)));
            item.put("p99Ms", round(samples.percentile(0.99)));
            item.put("maxMs", round(samples.max()));
            list.add(item);
        });
        list.sort(Comparator.comparingDouble((Map<String, Object> m) -> (double) m.get("p95Ms")).reversed());
        return list.size() > topN ? list.subList(0, topN) : list;
    }

    public List<Map<String, Object>> featureUsage(int topN) {
        List<Map<String, Object>> list = new ArrayList<>();
        requestSamples.forEach((path, samples) -> {
            long count = samples.count();
            if (count == 0) {
                return;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", path);
            item.put("count", count);
            list.add(item);
        });
        list.sort(Comparator.comparingLong((Map<String, Object> m) -> (long) m.get("count")).reversed());
        return list.size() > topN ? list.subList(0, topN) : list;
    }

    public Map<String, Object> overview() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalRequests", totalRequests.sum());
        map.put("distinctApis", requestSamples.size());
        map.put("slowApis", slowApis(10));
        map.put("featureUsage", featureUsage(10));
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }

    /* ==================== AI 指标 ==================== */

    public void recordAiStart() {
        aiInflight.incrementAndGet();
        aiRequests.increment();
    }

    public void recordAiEnd() {
        aiInflight.decrementAndGet();
    }

    public void recordAiLatency(String model, long totalMs, long firstTokenMs) {
        aiTimers.computeIfAbsent("tlias.ai.chat|" + model,
                key -> newAiTimer("tlias.ai.chat", model)).record(Duration.ofMillis(totalMs));
        aiSamples.computeIfAbsent(model, key -> new SampleBuffer()).add(totalMs);

        if (firstTokenMs > 0) {
            aiTimers.computeIfAbsent("tlias.ai.chat.ttft|" + model,
                    key -> newAiTimer("tlias.ai.chat.ttft", model)).record(Duration.ofMillis(firstTokenMs));
            aiTtftSamples.computeIfAbsent(model, key -> new SampleBuffer()).add(firstTokenMs);
        }
    }

    private Timer newAiTimer(String name, String model) {
        return Timer.builder(name)
                .tag("model", model)
                .publishPercentileHistogram()
                .register(registry);
    }

    /**
     * @param outcome success | error | aborted | rate_limited
     */
    public void recordAiOutcome(String model, String outcome) {
        Counter.builder("tlias.ai.requests")
                .tag("model", model)
                .tag("outcome", outcome)
                .description("AI 对话结果计数")
                .register(registry)
                .increment();
    }

    /** 流式响应出错后的兜底回答次数，非 0 说明上游在抖 */
    public void recordAiFallback(String reason) {
        Counter.builder("tlias.ai.fallback")
                .tag("reason", reason)
                .description("AI 兜底回答次数")
                .register(registry)
                .increment();
    }

    public void recordKbRetrieval(boolean hit) {
        Counter.builder("tlias.ai.kb.retrieval")
                .tag("result", hit ? "hit" : "miss")
                .description("RAG 检索命中情况")
                .register(registry)
                .increment();
    }

    public void recordRateLimited() {
        Counter.builder("tlias.ai.ratelimited")
                .description("被限流拒绝的请求数")
                .register(registry)
                .increment();
    }

    public void recordTokens(Number prompt, Number completion) {
        long promptDelta = prompt == null ? 0 : Math.max(prompt.longValue(), 0);
        long completionDelta = completion == null ? 0 : Math.max(completion.longValue(), 0);
        promptTokens.add(promptDelta);
        completionTokens.add(completionDelta);
        if (promptDelta > 0) {
            Counter.builder("tlias.ai.tokens").tag("type", "prompt").register(registry).increment(promptDelta);
        }
        if (completionDelta > 0) {
            Counter.builder("tlias.ai.tokens").tag("type", "completion").register(registry).increment(completionDelta);
        }
        double cost = promptDelta / 1_000_000.0 * inputPricePerMillion
                + completionDelta / 1_000_000.0 * outputPricePerMillion;
        if (cost > 0) {
            Counter.builder("tlias.ai.cost")
                    .description("按配置单价估算的调用成本（元）")
                    .register(registry)
                    .increment(cost);
        }
    }

    public Map<String, Object> aiSnapshot() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("aiRequests", aiRequests.sum());
        map.put("kbHitRate", kbRate(true));
        map.put("kbFallbackRate", kbRate(false));
        map.put("avgResponseMs", round(aggregate(aiSamples, Agg.AVG)));
        map.put("p50ResponseMs", round(aggregate(aiSamples, Agg.P50)));
        map.put("p95ResponseMs", round(aggregate(aiSamples, Agg.P95)));
        map.put("p99ResponseMs", round(aggregate(aiSamples, Agg.P99)));
        map.put("avgFirstTokenMs", round(aggregate(aiTtftSamples, Agg.AVG)));
        map.put("p95FirstTokenMs", round(aggregate(aiTtftSamples, Agg.P95)));
        map.put("promptTokens", promptTokens.sum());
        map.put("completionTokens", completionTokens.sum());
        map.put("totalTokens", promptTokens.sum() + completionTokens.sum());
        map.put("estimatedCostCny", round(counterTotal("tlias.ai.cost")));
        map.put("rateLimitedCount", Math.round(counterTotal("tlias.ai.ratelimited")));
        map.put("fallbackCount", Math.round(counterTotal("tlias.ai.fallback")));
        map.put("inflight", aiInflight.get());
        map.put("inputPricePerMillion", inputPricePerMillion);
        map.put("outputPricePerMillion", outputPricePerMillion);
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }

    private enum Agg { AVG, P50, P95, P99 }

    /**
     * 多模型混跑时取所有样本合并后的分位数，比“每个模型各自算再取最大”更接近真实体验。
     */
    private double aggregate(Map<String, SampleBuffer> buffers, Agg agg) {
        List<Long> all = new ArrayList<>();
        buffers.values().forEach(buffer -> buffer.copyInto(all));
        if (all.isEmpty()) {
            return 0.0;
        }
        long[] values = all.stream().mapToLong(Long::longValue).toArray();
        Arrays.sort(values);
        return switch (agg) {
            case AVG -> Arrays.stream(values).average().orElse(0);
            case P50 -> values[(int) Math.round(0.50 * (values.length - 1))];
            case P95 -> values[(int) Math.round(0.95 * (values.length - 1))];
            case P99 -> values[(int) Math.round(0.99 * (values.length - 1))];
        };
    }

    private double kbRate(boolean hit) {
        double hitCount = counterTotal("tlias.ai.kb.retrieval", "result", "hit");
        double missCount = counterTotal("tlias.ai.kb.retrieval", "result", "miss");
        double sum = hitCount + missCount;
        return sum == 0 ? 0.0 : round((hit ? hitCount : missCount) * 100.0 / sum);
    }

    private double counterTotal(String name, String... tags) {
        var search = registry.find(name);
        for (int i = 0; i + 1 < tags.length; i += 2) {
            search = search.tag(tags[i], tags[i + 1]);
        }
        return search.counters().stream().mapToDouble(Counter::count).sum();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** 定长环形缓冲，只用于进程内分位数计算，不参与 Prometheus 导出 */
    private static final class SampleBuffer {

        private final long[] values = new long[MAX_SAMPLES];
        private int size;
        private int next;

        synchronized void add(long value) {
            values[next] = value;
            next = (next + 1) % MAX_SAMPLES;
            if (size < MAX_SAMPLES) {
                size++;
            }
        }

        synchronized long count() {
            return size;
        }

        synchronized void copyInto(List<Long> target) {
            for (int i = 0; i < size; i++) {
                target.add(values[i]);
            }
        }

        synchronized double percentile(double p) {
            if (size == 0) {
                return 0;
            }
            long[] copy = Arrays.copyOf(values, size);
            Arrays.sort(copy);
            return copy[(int) Math.round(p * (size - 1))];
        }

        synchronized double average() {
            if (size == 0) {
                return 0;
            }
            long sum = 0;
            for (int i = 0; i < size; i++) {
                sum += values[i];
            }
            return (double) sum / size;
        }

        synchronized long max() {
            long max = 0;
            for (int i = 0; i < size; i++) {
                max = Math.max(max, values[i]);
            }
            return max;
        }
    }
}
