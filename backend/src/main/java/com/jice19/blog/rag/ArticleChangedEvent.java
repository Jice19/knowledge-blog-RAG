package com.jice19.blog.rag;

/**
 * 文章变更事件（发布/编辑/删除），事务提交后转成 RabbitMQ 消息
 */
public record ArticleChangedEvent(Long articleId, String action) {
}
