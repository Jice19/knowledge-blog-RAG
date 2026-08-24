package com.jice19.blog.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.jice19.blog.config.RagProperties;
import com.jice19.blog.entity.Article;
import com.jice19.blog.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 核心服务：入库、检索、问答（检索增强生成）。
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final ChatService chatService;
    private final RagProperties props;
    private final ArticleMapper articleMapper;

    /**
     * 全量入库：所有「已发布」文章 → 按标题切片 → 向量化 → 存入 Qdrant。
     * 返回入库的 chunk 数量。
     */
    public int ingestAll() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1));
        vectorStoreService.ensureCollection(props.getCollection());

        List<Map<String, Object>> points = new ArrayList<>();
        long seq = 1;
        for (Article a : articles) {
            for (Chunk c : chunkService.chunk(a.getContent())) {
                float[] vec = embeddingService.embed(c.text());
                Map<String, Object> payload = Map.of(
                        "articleId", a.getId(),
                        "articleTitle", a.getTitle(),
                        "heading", c.headingPath(),
                        "text", c.text());
                points.add(Map.of("id", seq++, "vector", vec, "payload", payload));
            }
        }
        if (!points.isEmpty()) {
            vectorStoreService.upsertPoints(props.getCollection(), points);
        }
        return points.size();
    }

    /** 检索：问题 → 向量化 → Qdrant 相似度 Top N */
    public List<JsonNode> search(String query, int topK) {
        float[] vec = embeddingService.embed(query);
        return vectorStoreService.search(props.getCollection(), vec, topK);
    }

    /**
     * 问答（RAG 闭环）：
     * 问题 → 检索 Top N chunk → 组装 prompt → LLM 生成答案 → 返回「答案 + 引用来源」
     */
    public AskResult ask(String question, int topK) {
        List<JsonNode> hits = search(question, topK);

        StringBuilder context = new StringBuilder();
        List<AskResult.Reference> refs = new ArrayList<>();
        int i = 1;
        for (JsonNode hit : hits) {
            JsonNode payload = hit.path("payload");
            String text = payload.path("text").asText();
            String title = payload.path("articleTitle").asText();
            String heading = payload.path("heading").asText();
            long articleId = payload.path("articleId").asLong();
            double score = hit.path("score").asDouble();

            context.append("【资料").append(i).append("】（来源：《").append(title)
                    .append("》- ").append(heading.isBlank() ? "全文" : "章节：" + heading)
                    .append("）\n").append(text).append("\n\n");
            refs.add(new AskResult.Reference(articleId, title, heading, score));
            i++;
        }

        String system = "你是技术知识库问答助手。请只根据提供的资料回答问题，不要编造资料中没有的内容；"
                + "如果资料不足以回答，请如实说明。用中文回答，简洁清晰。";
        String user = "以下是从知识库检索到的相关资料：\n\n" + context + "问题：" + question;

        String answer = chatService.chat(system, user);
        return new AskResult(answer, refs);
    }
}
