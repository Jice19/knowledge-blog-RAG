package com.jice19.blog.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.jice19.blog.config.RagProperties;
import com.jice19.blog.config.RestClientConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 大模型对话服务：调用 Ollama /api/chat 生成回答（qwen3:1.7b）。
 */
@Service
public class ChatService {

    private final RestClient client;
    private final RagProperties props;

    public ChatService(RagProperties props, RestClientConfig restClientConfig) {
        this.props = props;
        this.client = restClientConfig.buildLocalRestClient(props.getOllamaUrl());
    }

    /** 单轮对话：system 定角色，user 给上下文+问题，返回助手回答 */
    public String chat(String system, String user) {
        Map<String, Object> body = Map.of(
                "model", props.getChatModel(),
                "stream", false,
                // 保持模型加载 30 分钟，避免空闲被卸载后重载（重载要 1~2 分钟）
                "keep_alive", "30m",
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                ));
        JsonNode resp = client.post()
                .uri("/api/chat")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (resp == null || resp.path("message").path("content").isMissingNode()) {
            throw new IllegalStateException("Ollama Chat 返回为空");
        }
        return resp.path("message").path("content").asText();
    }
}
