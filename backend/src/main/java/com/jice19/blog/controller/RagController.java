package com.jice19.blog.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jice19.blog.common.Result;
import com.jice19.blog.rag.AskResult;
import com.jice19.blog.rag.ChatService;
import com.jice19.blog.rag.RagService;
import com.jice19.blog.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * RAG 接口：入库 / 检索 / 问答（含 SSE 流式 + 多轮会话）
 */
@RestController
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final ChatService chatService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    /** 全量入库（管理端触发，需登录） */
    @PostMapping("/api/admin/rag/ingest")
    public Result<Integer> ingest() {
        return Result.success(ragService.ingestAll());
    }

    /** 检索测试 */
    @GetMapping("/api/rag/search")
    public Result<List<JsonNode>> search(@RequestParam String q, @RequestParam(defaultValue = "3") int topK) {
        return Result.success(ragService.search(q, topK));
    }

    /** 非流式问答（conversationId 可选：多轮会话时传，历史从库读并保存消息） */
    @GetMapping("/api/rag/ask")
    public Result<AskResult> ask(@RequestParam String q, @RequestParam(defaultValue = "3") int topK,
                                 @RequestParam(required = false) Long conversationId,
                                 @RequestParam(required = false) String history) {
        List<Map<String, String>> hist = conversationId != null
                ? conversationService.history(conversationId, 5)
                : parseHistory(history);
        AskResult result = ragService.ask(q, topK, hist);
        if (conversationId != null) {
            conversationService.addMessage(conversationId, "user", q, null);
            conversationService.updateTitleIfDefault(conversationId, q);
            conversationService.addMessage(conversationId, "assistant", result.answer(),
                    toJson(result.references()));
        }
        return Result.success(result);
    }

    /** SSE 流式问答（多轮会话：传 conversationId，先存 user 消息，生成后存 assistant 消息） */
    @GetMapping("/api/rag/ask/stream")
    public SseEmitter askStream(@RequestParam String q, @RequestParam(defaultValue = "3") int topK,
                                @RequestParam(required = false) Long conversationId,
                                @RequestParam(required = false) String history) {
        SseEmitter emitter = new SseEmitter(300_000L);
        List<Map<String, String>> hist = conversationId != null
                ? conversationService.history(conversationId, 5)
                : parseHistory(history);
        CompletableFuture.runAsync(() -> {
            try {
                RagService.AskContext ctx = ragService.buildContext(q, topK, hist);
                if (conversationId != null) {
                    conversationService.addMessage(conversationId, "user", q, null);
                    conversationService.updateTitleIfDefault(conversationId, q);
                }
                StringBuilder collector = new StringBuilder();
                chatService.chatStream(ctx.system(), ctx.user(), emitter, collector);
                emitter.send(SseEmitter.event().name("references").data(ctx.references()));
                if (conversationId != null) {
                    conversationService.addMessage(conversationId, "assistant",
                            collector.toString(), toJson(ctx.references()));
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private List<Map<String, String>> parseHistory(String history) {
        if (history == null || history.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(history, new TypeReference<List<Map<String, String>>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
