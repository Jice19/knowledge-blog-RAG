---
title: RabbitMQ 交换机类型：direct topic fanout
summary: 三种交换机如何按不同规则把消息路由到队列
category: RabbitMQ
---

## 交换机的作用

生产者不直接把消息发给队列，而是发给交换机，交换机根据绑定规则把消息路由到一个或多个队列。理解交换机是理解 RabbitMQ 路由的关键。

## direct 与 fanout

direct 交换机按路由键精确匹配，路由键和绑定键完全一致才投递，适合点对点任务。fanout 交换机忽略路由键，把消息广播到所有绑定的队列，适合通知、日志广播场景。

## topic 交换机

topic 交换机支持通配符匹配，绑定键可以用星号匹配一个词、井号匹配零个或多个词。例如 order.# 能匹配 order.created 和 order.paid。它适合按主题灵活路由的多消费者场景。本项目用的是 direct 交换机，路由键固定为队列名，简单清晰。
