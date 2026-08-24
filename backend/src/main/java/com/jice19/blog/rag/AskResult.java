package com.jice19.blog.rag;

import java.util.List;

/**
 * RAG 问答结果：答案 + 引用来源列表
 */
public record AskResult(String answer, List<Reference> references) {

    /** 引用来源：哪篇文章的哪个章节，以及相似度分数 */
    public record Reference(Long articleId, String articleTitle, String heading, double score) {
    }
}
