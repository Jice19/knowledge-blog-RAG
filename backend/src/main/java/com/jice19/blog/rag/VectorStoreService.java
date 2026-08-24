package com.jice19.blog.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.jice19.blog.config.RagProperties;
import com.jice19.blog.config.RestClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 向量库服务：通过 Qdrant REST API 建集合、入库、检索。
 * 关键：打印所有请求的原始响应体 + 建集合后强制验证，杜绝"静默成功"。
 */
@Slf4j
@Service
public class VectorStoreService {

    private final RestClient client;
    private final RagProperties props;

    public VectorStoreService(RagProperties props, RestClientConfig restClientConfig) {
        this.props = props;
        this.client = restClientConfig.buildLocalRestClient(props.getQdrantUrl());
        log.info("[RAG] VectorStoreService 初始化，Qdrant 地址: {}（HttpURLConnection）", props.getQdrantUrl());
    }

    /** 建集合：GET 检查 → PUT 创建（打印原始响应）→ GET 验证（失败即抛异常） */
    public void ensureCollection(String name) {
        try {
            String resp = client.get().uri("/collections/{name}", name).retrieve().body(String.class);
            log.info("[RAG] 集合已存在: {} | {}", name, resp);
            return;
        } catch (Exception e) {
            log.info("[RAG] GET 检查集合（404 属正常）: {}", e.getMessage());
        }

        Map<String, Object> body = Map.of(
                "vectors", Map.of("size", props.getVectorSize(), "distance", "Cosine"));
        try {
            String resp = client.put()
                    .uri("/collections/{name}", name)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            log.info("[RAG] PUT 建集合原始响应: {}", resp);
        } catch (Exception e) {
            log.error("[RAG] PUT 建集合失败: {}", e.getMessage(), e);
            throw e;
        }

        // 验证：集合必须真的存在
        try {
            client.get().uri("/collections/{name}", name).retrieve().body(String.class);
            log.info("[RAG] 验证通过：集合已真实存在 ✓ {}", name);
        } catch (Exception e) {
            log.error("[RAG] 验证失败：PUT 后集合仍不存在！{}", e.getMessage());
            throw new IllegalStateException("建集合后验证失败（PUT 未真正生效）: " + e.getMessage());
        }
    }

    /** 批量入库（upsert，打印原始响应） */
    public void upsertPoints(String collection, List<Map<String, Object>> points) {
        log.info("[RAG] 开始入库 {} 个点", points.size());
        Map<String, Object> body = Map.of("points", points);
        try {
            String resp = client.put()
                    .uri("/collections/{c}/points?wait=true", collection)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            log.info("[RAG] 入库原始响应: {}", resp);
        } catch (Exception e) {
            log.error("[RAG] 入库失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /** 相似度检索：返回 Top N 命中的 payload */
    public List<JsonNode> search(String collection, float[] vector, int topK) {
        Map<String, Object> body = Map.of(
                "vector", vector,
                "limit", topK,
                "with_payload", true);
        JsonNode resp = client.post()
                .uri("/collections/{c}/points/search", collection)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (resp == null) {
            return List.of();
        }
        JsonNode result = resp.path("result");
        List<JsonNode> hits = new ArrayList<>();
        result.forEach(hits::add);
        return hits;
    }
}
