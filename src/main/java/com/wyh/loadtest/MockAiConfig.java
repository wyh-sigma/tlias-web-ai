package com.wyh.loadtest;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 仅 loadtest profile 生效：用假模型替换真实模型。
 *
 * <p>启动方式：
 * <pre>
 *   SPRING_PROFILES_ACTIVE=loadtest java -jar tlias-web-ai.jar
 *   或者 docker compose --profile loadtest up -d app-loadtest
 * </pre>
 *
 * <p>这样 AI 链路可以被高并发压测，而不消耗 token、不受供应商限流影响。
 */
@Configuration
@Profile("loadtest")
public class MockAiConfig {

    @Bean
    @Primary
    public ChatModel mockChatModel(
            @Value("${tlias.loadtest.chunk-delay-ms:20}") long chunkDelayMs,
            @Value("${tlias.loadtest.chunk-count:30}") int chunkCount,
            LoadtestFaultState faultState) {
        return new MockChatModel(chunkDelayMs, chunkCount, faultState);
    }

    @Bean
    @Primary
    public EmbeddingModel mockEmbeddingModel() {
        return new MockEmbeddingModel();
    }
}
