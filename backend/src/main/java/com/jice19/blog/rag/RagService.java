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
 * RAG 核心服务：文章入库（切片→向量化→存 Qdrant）与检索。
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
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
}
