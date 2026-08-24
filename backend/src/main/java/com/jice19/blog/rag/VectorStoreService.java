package com.jice19.blog.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.jice19.blog.config.RagProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 向量库服务：通过 Qdrant REST API 建集合、入库、检索。
 */
@Service
public class VectorStoreService {

    private final RestClient client;
    private final RagProperties props;

    public VectorStoreService(RagProperties props) {
        this.props = props;
        this.client = RestClient.builder().baseUrl(props.getQdrantUrl()).build();
    }

    /** 建集合（若不存在） */
    public void ensureCollection(String name) {
        Map<String, Object> body = Map.of(
                "vectors", Map.of("size", props.getVectorSize(), "distance", "Cosine"));
        client.put()
                .uri("/collections/{name}", name)
                .body(body)
                .retrieve();
    }

    /** 批量入库（upsert） */
    public void upsertPoints(String collection, List<Map<String, Object>> points) {
        Map<String, Object> body = Map.of("points", points);
        client.put()
                .uri("/collections/{c}/points?wait=true", collection)
                .body(body)
                .retrieve();
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
        java.util.List<JsonNode> hits = new java.util.ArrayList<>();
        result.forEach(hits::add);
        return hits;
    }
}
