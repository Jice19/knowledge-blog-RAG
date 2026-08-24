package com.jice19.blog.rag;

/**
 * 异步向量化任务消息：只传文章 id 与动作，内容由消费者从库读取
 */
public record RagArticleMessage(Long articleId, String action) {

    public static final String INGEST = "INGEST";
    public static final String DELETE = "DELETE";
}
