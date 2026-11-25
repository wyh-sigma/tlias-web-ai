package com.wyh.service;


import reactor.core.publisher.Flux;


public interface AiChatService {

    /**
     * 流式对话
     *
     * @param conversationId 会话ID，用于隔离多轮对话上下文
     * @param userMsg        用户消息
     */
    Flux<String> chat(String conversationId, String userMsg);

    /**
     * 清空指定会话的上下文
     */
    void clear(String conversationId);
}
