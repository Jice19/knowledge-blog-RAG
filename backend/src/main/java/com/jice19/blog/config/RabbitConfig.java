package com.jice19.blog.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置：主队列（带死信）+ 死信队列 + JSON 消息转换
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "rag.exchange";
    public static final String QUEUE = "rag.article";
    public static final String DLX = "rag.exchange.dlx";
    public static final String DLQ = "rag.article.dlq";

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange ragExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange ragDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue ragQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX);
        args.put("x-dead-letter-routing-key", DLQ);
        return new Queue(QUEUE, true, false, false, args);
    }

    @Bean
    public Queue ragDlq() {
        return new Queue(DLQ, true);
    }

    @Bean
    public Binding ragBinding() {
        return BindingBuilder.bind(ragQueue()).to(ragExchange()).with(QUEUE);
    }

    @Bean
    public Binding ragDlqBinding() {
        return BindingBuilder.bind(ragDlq()).to(ragDlx()).with(DLQ);
    }
}
