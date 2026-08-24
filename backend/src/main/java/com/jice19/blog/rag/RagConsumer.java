package com.jice19.blog.rag;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 向量化任务消费者：异步处理文章入库/删除
 * 手动 ack：业务成功才 basicAck；失败抛异常 → Spring Retry 重试 3 次 → 耗尽后拒绝进死信队列（不丢消息）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagConsumer {

    private final RagService ragService;

    @RabbitListener(queues = "rag.article", ackMode = "MANUAL")
    public void onMessage(RagArticleMessage msg, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            log.info("[RAG] 消费消息: articleId={}, action={}", msg.articleId(), msg.action());
            if (RagArticleMessage.INGEST.equals(msg.action())) {
                ragService.ingestArticle(msg.articleId());
            } else {
                ragService.removeArticle(msg.articleId());
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("[RAG] 消费失败，交由重试/死信处理: articleId={}, action={}, err={}",
                    msg.articleId(), msg.action(), e.getMessage());
            throw e;
        }
    }
}
