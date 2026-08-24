package com.jice19.blog.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.jice19.blog.config.RagProperties;
import com.jice19.blog.config.RestClientConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Embedding 服务：调用 Ollama 的 /api/embed 把文本向量化。
 * 使用不走代理的 RestClient（直连本机 Ollama）。
 */
@Service
public class EmbeddingService {

    private final RestClient client;
    private final RagProperties props;

    public EmbeddingService(RagProperties props, RestClientConfig restClientConfig) {
        this.props = props;
        this.client = restClientConfig.buildLocalRestClient(props.getOllamaUrl());
    }

    /** 把文本转成向量 */
    public float[] embed(String text) {
        Map<String, Object> body = Map.of(
                "model", props.getEmbeddingModel(),
                "input", text);
        JsonNode resp = client.post()
                .uri("/api/embed")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (resp == null || resp.path("embeddings").isEmpty()) {
            throw new IllegalStateException("Ollama Embedding 返回为空");
        }
        JsonNode arr = resp.path("embeddings").get(0);
        float[] vec = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            vec[i] = (float) arr.get(i).asDouble();
        }
        return vec;
    }
}
