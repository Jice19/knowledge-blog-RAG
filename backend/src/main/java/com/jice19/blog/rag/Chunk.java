package com.jice19.blog.rag;

/**
 * 切片结果：正文 + 标题路径（用于引用溯源）
 */
public record Chunk(String text, String headingPath) {
}
