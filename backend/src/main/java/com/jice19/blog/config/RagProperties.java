package com.jice19.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 相关配置（application.yml 的 rag.* 前缀）
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /** Ollama 服务地址 */
    private String ollamaUrl = "http://localhost:11434";

    /** Qdrant 服务地址 */
    private String qdrantUrl = "http://localhost:6333";

    /** Embedding 模型 */
    private String embeddingModel = "mxbai-embed-large";

    /** 生成模型 */
    private String chatModel = "qwen3:1.7b";

    /** Qdrant 集合名 */
    private String collection = "blog_chunks";

    /** 向量维度 */
    private int vectorSize = 1024;

    /** 标题切片超过该字符数时按句子二次切分 */
    private int chunkMaxChars = 1000;

    /** 二次切分相邻片段的重叠字符数 */
    private int chunkOverlapChars = 100;
}
