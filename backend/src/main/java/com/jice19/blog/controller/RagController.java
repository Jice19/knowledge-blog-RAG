package com.jice19.blog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.jice19.blog.common.Result;
import com.jice19.blog.rag.AskResult;
import com.jice19.blog.rag.ChatService;
import com.jice19.blog.rag.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RAG 接口：入库 / 检索 / 问答（含 SSE 流式）
 */
@RestController
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final ChatService chatService;

    /** 全量入库（管理端触发，需登录） */
    @PostMapping("/api/admin/rag/ingest")
    public Result<Integer> ingest() {
        return Result.success(ragService.ingestAll());
    }

    /** 检索测试（返回 Top N 命中的 chunk） */
    @GetMapping("/api/rag/search")
    public Result<List<JsonNode>> search(@RequestParam String q, @RequestParam(defaultValue = "3") int topK) {
        return Result.success(ragService.search(q, topK));
    }

    /** 非流式问答 */
    @GetMapping("/api/rag/ask")
    public Result<AskResult> ask(@RequestParam String q, @RequestParam(defaultValue = "3") int topK) {
        return Result.success(ragService.ask(q, topK));
    }

    /**
     * SSE 流式问答：
     * 先逐字推 token 事件，完成后推 references（引用来源）事件，再结束。
     */
    @GetMapping("/api/rag/ask/stream")
    public SseEmitter askStream(@RequestParam String q, @RequestParam(defaultValue = "3") int topK) {
        SseEmitter emitter = new SseEmitter(300_000L);
        CompletableFuture.runAsync(() -> {
            try {
                RagService.AskContext ctx = ragService.buildContext(q, topK);
                chatService.chatStream(ctx.system(), ctx.user(), emitter);
                emitter.send(SseEmitter.event().name("references").data(ctx.references()));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
