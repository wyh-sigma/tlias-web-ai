package com.wyh.service.impl;

import com.wyh.monitor.MetricsRegistry;
import com.wyh.filter.TraceIdFilter;
import com.wyh.security.AuthUser;
import com.wyh.service.AiChatService;
import com.wyh.utils.TliasAIQueryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.MDC;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 对话实现。
 *
 * <p>指标采集点（对应压测关注的四个维度）：
 * <ul>
 *   <li>延迟：总耗时与首字延迟（TTFT）分别打点，都带模型标签</li>
 *   <li>流量：recordAiStart/End 维护并发数</li>
 *   <li>错误：按 success/error/aborted 统计结果，兜底次数单独计数</li>
 *   <li>成本：token 数换算成金额，写入 tlias.ai.cost</li>
 * </ul>
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String DEFAULT_CONVERSATION_ID = "default";

    private final ChatClient chatClient;
    private final PromptTemplate tliasAssistantPrompt;
    private final TliasAIQueryUtils tliasAIQueryUtils;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final MessageChatMemoryAdvisor messageChatMemoryAdvisor;
    private final ChatMemory chatMemory;
    private final MetricsRegistry metricsRegistry;
    /** 指标标签用的模型名；loadtest profile 下会被覆盖成 mock-loadtest */
    private final String modelName;

    public AiChatServiceImpl(ChatClient.Builder builder,
                             PromptTemplate tliasAssistantPrompt,
                             TliasAIQueryUtils tliasAIQueryUtils,
                             RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
                             MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                             ChatMemory chatMemory,
                             MetricsRegistry metricsRegistry,
                             @Value("${tlias.ai.model-name:${spring.ai.deepseek.chat.options.model:deepseek-chat}}")
                             String modelName) {
        this.chatClient = builder.build();
        this.tliasAssistantPrompt = tliasAssistantPrompt;
        this.tliasAIQueryUtils = tliasAIQueryUtils;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.messageChatMemoryAdvisor = messageChatMemoryAdvisor;
        this.chatMemory = chatMemory;
        this.metricsRegistry = metricsRegistry;
        this.modelName = modelName;
    }

    @Override
    public Flux<String> chat(String conversationId, String userMsg) {
        AuthUser user = currentUser();
        // 会话按用户隔离，避免不同用户使用相同 conversationId 时串上下文
        String conversation = scoped(conversationId, user);
        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("userId", user == null ? -1L : user.id());
        toolContext.put("role", user == null ? "USER" : user.role());

        String systemText = tliasAssistantPrompt.render();
        long startTime = System.currentTimeMillis();
        // 流式回调发生在 Reactor 线程上，ThreadLocal 形式的 MDC 不会自动带过去，
        // 这里先取出 traceId，写日志时再放回去，保证 JSON 日志能和入口请求对上。
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        // 首字延迟：第一个非空文本块到达的时间
        AtomicLong firstTokenAt = new AtomicLong(0);
        // 出错后 onErrorResume 会补一段兜底文本，信号会变成 ON_COMPLETE，
        // 所以单独记一个标记，保证结果分布里仍统计为 error
        AtomicBoolean errored = new AtomicBoolean(false);
        metricsRegistry.recordAiStart();

        return chatClient.prompt()
                .system(systemText)
                .user(userMsg)
                // 记忆 advisor 先补充历史消息，RAG advisor 再检索知识库增强上下文
                .advisors(spec -> spec
                        .advisors(messageChatMemoryAdvisor, retrievalAugmentationAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, conversation))
                .tools(tliasAIQueryUtils)
                // 把当前用户身份传给工具，做数据级权限控制
                .toolContext(toolContext)
                .stream()
                .chatResponse()
                .doOnNext(response -> collectUsage(response, usageRef))
                .map(this::extractText)
                .filter(text -> !text.isEmpty())
                .doOnNext(text -> firstTokenAt.compareAndSet(0, System.currentTimeMillis()))
                // 不管正常结束、报错还是客户端点停止（CANCEL），都要收尾，否则并发数和耗时会漏记
                .doFinally(signal -> finish(conversation, startTime, firstTokenAt, usageRef, errored, traceId, signal))
                .doOnError(e -> {
                    errored.set(true);
                    log.error("AI 对话失败，conversationId={}", conversation, e);
                })
                // 流式接口出错时兜底，避免前端只收到半截响应
                .onErrorResume(e -> {
                    errored.set(true);
                    metricsRegistry.recordAiFallback(classify(e));
                    return Flux.just("抱歉，AI 服务暂时不可用，请稍后重试。");
                });
    }

    @Override
    public void clear(String conversationId) {
        chatMemory.clear(scoped(conversationId, currentUser()));
    }

    private void finish(String conversation, long startTime, AtomicLong firstTokenAt,
                        AtomicReference<Usage> usageRef, AtomicBoolean errored, String traceId, SignalType signal) {
        long totalMs = System.currentTimeMillis() - startTime;
        long firstTokenMs = firstTokenAt.get() > 0 ? firstTokenAt.get() - startTime : 0;

        metricsRegistry.recordAiLatency(modelName, totalMs, firstTokenMs);
        Usage usage = usageRef.get();
        if (usage != null) {
            metricsRegistry.recordTokens(usage.getPromptTokens(), usage.getCompletionTokens());
        }
        metricsRegistry.recordAiOutcome(modelName, errored.get() ? "error" : outcomeOf(signal));
        metricsRegistry.recordAiEnd();

        if (traceId != null) {
            MDC.put(TraceIdFilter.TRACE_ID, traceId);
        }
        try {
            if (usage == null) {
                log.info("AI 对话结束，conversationId={}, 耗时={}ms, 首字={}ms, signal={}, token 用量未返回",
                        conversation, totalMs, firstTokenMs, signal);
            } else {
                log.info("AI 对话结束，conversationId={}, 耗时={}ms, 首字={}ms, signal={}, promptTokens={}, completionTokens={}",
                        conversation, totalMs, firstTokenMs, signal,
                        usage.getPromptTokens(), usage.getCompletionTokens());
            }
        } finally {
            if (traceId != null) {
                MDC.remove(TraceIdFilter.TRACE_ID);
            }
        }
    }

    private String outcomeOf(SignalType signal) {
        return switch (signal) {
            case ON_COMPLETE -> "success";
            case CANCEL -> "aborted";
            default -> "error";
        };
    }

    /** 兜底原因分类，避免把异常类名直接当标签造成基数爆炸 */
    private String classify(Throwable e) {
        // 异常常被 Reactor / 聚合器包装，要沿 cause 链找真正的错误
        for (Throwable t = e; t != null; t = t.getCause()) {
            String name = t.getClass().getSimpleName().toLowerCase();
            String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
            if (name.contains("timeout") || msg.contains("timeout") || msg.contains("timed out")) {
                return "timeout";
            }
            if (msg.contains("429") || msg.contains("rate limit") || msg.contains("too many")) {
                return "rate_limit";
            }
            if (name.contains("abort") || msg.contains("abort")) {
                return "stream_abort";
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return "error";
    }

    /**
     * 流式响应中只取文本增量；工具调用等没有文本的块直接过滤掉
     */
    private String extractText(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        String text = chatResponse.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    /**
     * 流式响应通常只有最后一块带 usage，取到有效值就覆盖
     */
    private void collectUsage(ChatResponse chatResponse, AtomicReference<Usage> usageRef) {
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage != null && usage.getTotalTokens() != null && usage.getTotalTokens() > 0) {
            usageRef.set(usage);
        }
    }

    private String normalize(String conversationId) {
        return (conversationId == null || conversationId.isBlank())
                ? DEFAULT_CONVERSATION_ID : conversationId;
    }

    /**
     * 会话 key 加上用户 id 前缀，实现会话级数据隔离
     */
    private String scoped(String conversationId, AuthUser user) {
        String owner = user == null || user.id() == null ? "anonymous" : user.id().toString();
        return owner + ":" + normalize(conversationId);
    }

    private AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser;
        }
        return null;
    }
}
