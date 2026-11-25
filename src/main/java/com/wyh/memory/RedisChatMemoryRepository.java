package com.wyh.memory;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于普通 Redis 的对话记忆仓库。
 * <p>
 * 不依赖 RediSearch/RedisJSON，使用 Redis List 存消息：
 * 每条消息编码为 "消息类型|Base64(文本)"，避免文本中的换行和分隔符破坏格式。
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "tlias:chat:memory:";
    private static final String CONVERSATION_KEY = "tlias:chat:memory:conversations";
    private static final Duration TTL = Duration.ofDays(7);
    private static final int MAX_MESSAGES = 20;
    private static final String SEPARATOR = "|";

    private final StringRedisTemplate redisTemplate;

    public RedisChatMemoryRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> ids = redisTemplate.opsForSet().members(CONVERSATION_KEY);
        return ids == null ? List.of() : new ArrayList<>(ids);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<String> rows = redisTemplate.opsForList().range(key(conversationId), 0, -1);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(this::deserialize)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = key(conversationId);
        redisTemplate.delete(key);

        if (messages == null || messages.isEmpty()) {
            redisTemplate.opsForSet().remove(CONVERSATION_KEY, conversationId);
            return;
        }

        List<String> rows = messages.stream()
                .filter(Objects::nonNull)
                .map(this::serialize)
                .toList();
        redisTemplate.opsForList().rightPushAll(key, rows);
        // 只保留最近 N 条，并设置过期时间，避免 Redis 无限增长
        redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
        redisTemplate.expire(key, TTL);
        redisTemplate.opsForSet().add(CONVERSATION_KEY, conversationId);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(key(conversationId));
        redisTemplate.opsForSet().remove(CONVERSATION_KEY, conversationId);
    }

    private String key(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    private String serialize(Message message) {
        String text = message.getText() == null ? "" : message.getText();
        String base64 = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        return message.getMessageType().name() + SEPARATOR + base64;
    }

    private Message deserialize(String row) {
        int index = row.indexOf(SEPARATOR);
        if (index < 0) {
            return null;
        }
        MessageType type = MessageType.valueOf(row.substring(0, index));
        String text = new String(
                Base64.getDecoder().decode(row.substring(index + 1)), StandardCharsets.UTF_8);
        return switch (type) {
            case USER -> new UserMessage(text);
            case ASSISTANT -> new AssistantMessage(text);
            case SYSTEM -> new SystemMessage(text);
            // 工具消息结构复杂，且不参与历史回放，直接忽略
            case TOOL -> null;
        };
    }
}
