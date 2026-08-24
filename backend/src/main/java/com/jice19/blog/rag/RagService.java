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
    private final SparseVectorService sparseVectorService;
    private final VectorStoreService vectorStoreService;
    private final ChatService chatService;
    private final RagProperties props;
    private final ArticleMapper articleMapper;

    /**
     * 全量入库：所有「已发布」文章 → 按标题切片 → 向量化 → 存入 Qdrant。
     */
    public int ingestAll() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1));
        vectorStoreService.ensureCollection(props.getCollection());

        List<Map<String, Object>> points = new ArrayList<>();
        long seq = 1;
        for (Article a : articles) {
            for (Chunk c : chunkService.chunk(a.getContent())) {
                points.add(buildPoint(seq++, c, a));
            }
        }
        if (!points.isEmpty()) {
            vectorStoreService.upsertPoints(props.getCollection(), points);
        }
        return points.size();
    }

    /** 单篇入库（幂等）：删该文章旧 chunk → 切片 → 向量化 → 入库 */
    public void ingestArticle(Long id) {
        Article a = articleMapper.selectById(id);
        if (a == null || a.getStatus() != 1) {
            return;
        }
        vectorStoreService.ensureCollection(props.getCollection());
        vectorStoreService.deleteByArticleId(props.getCollection(), id);

        List<Map<String, Object>> points = new ArrayList<>();
        List<Chunk> chunks = chunkService.chunk(a.getContent());
        for (int i = 0; i < chunks.size(); i++) {
            points.add(buildPoint(id * 1000L + i, chunks.get(i), a));
        }
        if (!points.isEmpty()) {
            vectorStoreService.upsertPoints(props.getCollection(), points);
        }
    }

    /** 删除某篇文章的全部向量 */
    public void removeArticle(Long id) {
        vectorStoreService.deleteByArticleId(props.getCollection(), id);
    }

    /** 检索：双路混合（BM25 关键词 + 向量语义，RRF 融合）；问题无有效分词时退化纯向量检索 */
    public List<JsonNode> search(String query, int topK) {
        float[] dense = embeddingService.embed(query);
        SparseVectorService.SparseVector sparse = sparseVectorService.encode(query);
        if (sparse.indices().length == 0) {
            return vectorStoreService.search(props.getCollection(), dense, topK);
        }
        return vectorStoreService.hybridSearch(props.getCollection(), dense,
                sparse.indices(), sparse.values(), topK);
    }

    /** 构建一个 point：稠密向量 + BM25 稀疏向量 + payload（标题/章节/原文） */
    private Map<String, Object> buildPoint(long id, Chunk c, Article a) {
        float[] vec = embeddingService.embed(c.text());
        SparseVectorService.SparseVector sp = sparseVectorService.encode(c.text());
        Map<String, Object> vector = Map.of(
                VectorStoreService.DENSE, (Object) vec,
                VectorStoreService.SPARSE, Map.of("indices", sp.indices(), "values", sp.values()));
        Map<String, Object> payload = Map.of(
                "articleId", a.getId(),
                "articleTitle", a.getTitle(),
                "heading", c.headingPath(),
                "text", c.text());
        return Map.of("id", id, "vector", vector, "payload", payload);
    }

    /**
     * 构建问答上下文：Query 改写（多轮）→ 检索 Top N → 组装 prompt → 返回上下文与引用来源。
     * 供「非流式 ask」与「流式 askStream」共用。
     */
    public AskContext buildContext(String question, int topK, List<Map<String, String>> history) {
        // 多轮改写：结合历史把问题改写成独立查询，提升检索召回；无历史则原样
        String query = chatService.rewriteQuery(question, history);
        List<JsonNode> hits = search(query, topK);

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
        return new AskContext(system, user, refs);
    }

    /** 非流式问答 */
    public AskResult ask(String question, int topK, List<Map<String, String>> history) {
        AskContext ctx = buildContext(question, topK, history);
        String answer = chatService.chat(ctx.system(), ctx.user());
        return new AskResult(answer, ctx.references());
    }

    /** 问答上下文（system / user prompt + 引用来源） */
    public record AskContext(String system, String user, List<AskResult.Reference> references) {
    }
}
