package com.wyh.loadtest;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * 压测专用的假模型：不发任何网络请求、不花钱，行为可控。
 *
 * <p>为什么需要它：真实模型有供应商限流、网络抖动和 token 费用，无法施加高并发，
 * 也复现不了 429 / 超时 / 断流这些故障。用它可以把**自己代码**的开销
 * （拦截器、Redis 记忆、RAG 检索、SSE 转发、线程占用）单独压出来。
 *
 * <p>故障注入通过最后一条用户消息里的标记触发：
 * <ul>
 *   <li>{@code [FAULT:429]} 直接抛限流错误</li>
 *   <li>{@code [FAULT:TIMEOUT]} 直接抛超时</li>
 *   <li>{@code [FAULT:ABORT]} 输出两块后中断流</li>
 * </ul>
 */
public class MockChatModel implements ChatModel {

    private static final String MODEL = "mock-loadtest";

    private final long chunkDelayMs;
    private final int chunkCount;
    private final LoadtestFaultState faultState;

    public MockChatModel(long chunkDelayMs, int chunkCount, LoadtestFaultState faultState) {
        this.chunkDelayMs = chunkDelayMs;
        this.chunkCount = chunkCount;
        this.faultState = faultState;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return response("这是压测用的模拟回答，不会调用真实模型。", usage(prompt), true);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        String text = lastUserText(prompt);
        LoadtestFaultState.Fault fault = faultState.get();

        // 开关优先（由 /loadtest/fault 设置），消息标记作为补充手段
        if (fault == LoadtestFaultState.Fault.RATE_LIMIT || text.contains("[FAULT:429]")) {
            return Flux.error(new IllegalStateException("429 Too Many Requests"));
        }
        if (fault == LoadtestFaultState.Fault.TIMEOUT || text.contains("[FAULT:TIMEOUT]")) {
            return Flux.error(new RuntimeException("request timeout"));
        }

        Flux<ChatResponse> chunks = Flux.range(0, chunkCount)
                .delayElements(Duration.ofMillis(chunkDelayMs))
                .map(i -> response("模拟第" + i + "段回答。", null, false));

        if (fault == LoadtestFaultState.Fault.ABORT || text.contains("[FAULT:ABORT]")) {
            return chunks.take(2).concatWith(Flux.error(new RuntimeException("stream aborted by upstream")));
        }
        // 最后一块带 usage，模拟真实供应商只在结尾返回 token 用量
        return chunks.concatWith(Flux.just(response("", usage(prompt), true)));
    }

    private String lastUserText(Prompt prompt) {
        List<Message> instructions = prompt.getInstructions();
        for (int i = instructions.size() - 1; i >= 0; i--) {
            if (instructions.get(i) instanceof UserMessage userMessage) {
                return userMessage.getText() == null ? "" : userMessage.getText();
            }
        }
        return "";
    }

    private ChatResponse response(String text, DefaultUsage usage, boolean last) {
        ChatResponseMetadata.Builder metadata = ChatResponseMetadata.builder().model(MODEL);
        if (usage != null) {
            metadata.usage(usage);
        }
        Generation generation = new Generation(new AssistantMessage(text));
        return new ChatResponse(List.of(generation), metadata.build());
    }

    private DefaultUsage usage(Prompt prompt) {
        String contents = prompt.getContents() == null ? "" : prompt.getContents();
        int promptTokens = Math.max(contents.length() / 4, 1);
        int completionTokens = Math.max(chunkCount * 6, 1);
        return new DefaultUsage(promptTokens, completionTokens, promptTokens + completionTokens);
    }
}
