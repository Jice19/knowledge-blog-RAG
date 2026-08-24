package com.jice19.blog.rag;

import com.jice19.blog.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 向量化任务生产者：文章变更（事务提交后）发消息到 RabbitMQ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagProducer {

    private final RabbitTemplate rabbitTemplate;

    /** 事务提交后才发消息，避免消费者读到未提交数据 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleChanged(ArticleChangedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.QUEUE,
                new RagArticleMessage(event.articleId(), event.action()));
        log.info("[RAG] 已发送消息: articleId={}, action={}", event.articleId(), event.action());
    }
}
