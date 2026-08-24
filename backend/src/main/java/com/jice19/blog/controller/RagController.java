package com.jice19.blog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.jice19.blog.common.Result;
import com.jice19.blog.rag.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RAG 测试接口（验证切片→向量→入库→检索链路）
 */
@RestController
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

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
}
