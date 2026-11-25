package com.wyh.controller;

import com.wyh.anno.Log;
import com.wyh.dto.ChatDTO;

import com.wyh.pojo.Result;
import com.wyh.service.AiChatService;
import com.wyh.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final KnowledgeBaseService knowledgeBaseService;

    public AiChatController(AiChatService aiChatService, KnowledgeBaseService knowledgeBaseService) {
        this.aiChatService = aiChatService;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping(value = "/chat", produces = "text/event-stream;charset=utf-8")
    public Flux<String> chat(@Valid @RequestBody ChatDTO dto) {
        return aiChatService.chat(dto.getConversationId(), dto.getMessage());
    }

    /**
     * 清空某个会话的多轮上下文
     */
    @DeleteMapping("/chat/{conversationId}")
    @Log
    public Result clear(@PathVariable String conversationId) {
        aiChatService.clear(conversationId);
        return Result.success();
    }

    /**
     * 知识库热更新：不重启服务，按当前知识文件重新建库
     */
    @PostMapping("/knowledge/rebuild")
    @Log
    public Result rebuildKnowledge() throws IOException {
        KnowledgeBaseService.RebuildResult result = knowledgeBaseService.rebuild();
        return Result.success(Map.of("files", result.files(), "chunks", result.chunks()));
    }
}
