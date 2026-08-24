package com.jice19.blog.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jice19.blog.config.RagProperties;
import com.jice19.blog.config.RestClientConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 大模型对话服务：调 Ollama /api/chat（qwen3:1.7b）。
 * 支持一次性生成（chat）与 SSE 流式逐 token 生成（chatStream）。
 */
@Service
public class ChatService {

    private final RestClient client;
    private final RagProperties props;
    private final ObjectMapper objectMapper;

    public ChatService(RagProperties props, RestClientConfig restClientConfig, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.client = restClientConfig.buildLocalRestClient(props.getOllamaUrl());
    }

    private static final String SYSTEM_REWRITE =
            "你是对话改写助手。请把用户当前问题改写成一条不依赖上下文的独立查询，"
            + "保留关键信息与实体，只输出改写结果，不要任何解释。";

    /**
     * 多轮 Query 改写：结合历史对话，把当前问题改写成独立查询（用于提升检索召回）。
     * 无历史时原样返回。
     */
    public String rewriteQuery(String question, List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return question;
        }
        StringBuilder sb = new StringBuilder("以下是对话历史：\n");
        for (Map<String, String> h : history) {
            sb.append("用户：").append(h.get("q")).append("\n");
            sb.append("助手：").append(h.get("a")).append("\n");
        }
        sb.append("当前问题：").append(question);
        return chat(SYSTEM_REWRITE, sb.toString()).trim();
    }

    /** 单轮对话（非流式）：system 定角色，user 给上下文+问题，返回助手回答 */
    public String chat(String system, String user) {
        Map<String, Object> body = Map.of(
                "model", props.getChatModel(),
                "stream", false,
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

    /**
     * 流式对话：请求 Ollama stream=true，解析 NDJSON 流，
     * 把每个 token 通过 SseEmitter（event 名 token）逐字推送给前端。
     */
    public void chatStream(String system, String user, SseEmitter emitter, StringBuilder collector) {
        Map<String, Object> body = Map.of(
                "model", props.getChatModel(),
                "stream", true,
                "keep_alive", "30m",
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                ));
        client.post()
                .uri("/api/chat")
                .body(body)
                .exchange((request, response) -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isBlank()) {
                                continue;
                            }
                            JsonNode node = objectMapper.readTree(line);
                            String content = node.path("message").path("content").asText();
                            boolean done = node.path("done").asBoolean(false);
                            if (!content.isEmpty()) {
                                emitter.send(SseEmitter.event().name("token").data(content));
                                if (collector != null) {
                                    collector.append(content);
                                }
                            }
                            if (done) {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                    return null;
                });
    }
}
