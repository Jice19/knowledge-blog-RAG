package com.jice19.blog.controller;

import com.jice19.blog.common.Result;
import com.jice19.blog.config.RagProperties;
import com.jice19.blog.rag.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RAG 诊断接口：查看实际配置 + 实测 Qdrant 连接/建集合
 */
@RestController
@RequiredArgsConstructor
public class RagDiagnosticController {

    private final RagProperties props;
    private final VectorStoreService vectorStoreService;

    @GetMapping("/api/rag/diag")
    public Result<Map<String, Object>> diag() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("qdrantUrl", props.getQdrantUrl());
        result.put("ollamaUrl", props.getOllamaUrl());
        result.put("collection", props.getCollection());
        result.put("vectorSize", props.getVectorSize());

        // 1. 用后端自己的 RestClient 列集合
        RestClient client = RestClient.builder().baseUrl(props.getQdrantUrl()).build();
        try {
            Object list = client.get().uri("/collections").retrieve().body(Object.class);
            result.put("当前集合列表", list);
        } catch (Exception e) {
            result.put("列集合失败", e.getMessage());
        }

        // 2. 实测 PUT 建一个测试集合
        try {
            client.put()
                    .uri("/collections/diag_test_coll")
                    .body(Map.of("vectors", Map.of("size", props.getVectorSize(), "distance", "Cosine")))
                    .retrieve();
            result.put("PUT建测试集合", "无异常(2xx)");
        } catch (Exception e) {
            result.put("PUT建测试集合失败", e.getMessage());
        }

        // 3. 再列一次，看测试集合在不在
        try {
            Object list = client.get().uri("/collections").retrieve().body(Object.class);
            result.put("PUT之后的集合列表", list);
        } catch (Exception e) {
            result.put("第二次列集合失败", e.getMessage());
        }

        // 4. 清理测试集合
        try {
            client.delete().uri("/collections/diag_test_coll").retrieve();
            result.put("清理测试集合", "OK");
        } catch (Exception e) {
            result.put("清理测试集合失败", e.getMessage());
        }
        return Result.success(result);
    }
}
