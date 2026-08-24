# 开发任务：RabbitMQ 异步向量化任务队列

> 关联：`task-article-auto-ingest.md`（发布时自动入库，本文将其异步机制升级为 RabbitMQ）
> 简历卖点：「基于 RabbitMQ 构建异步向量化任务队列，文章发布与向量入库解耦，发布接口秒回；手动 ack + 重试 + 死信队列保障消息可靠性」
> 优先级：高（后端思维体现 · 简历原话）

---

## 1. 背景与目标

**现状**：文章入库要么手动触发 ingest，要么依赖线程池异步（进程内，重启丢任务、无法重试）。

**目标**：用 RabbitMQ 做**跨进程可靠异步**：
- 文章**发布/编辑** → 消息 → 消费者异步切片向量化入库
- 文章**下线/删除** → 消息 → 消费者异步移除向量
- 发布接口**秒回**；任务失败**自动重试**，最终失败进**死信队列**兜底

---

## 2. 需求（按点）

1. 引入 RabbitMQ（Docker 部署 + Spring AMQP 依赖）
2. 文章保存/删除后（事务提交）发布消息：`{articleId, action}`，action ∈ `INGEST / DELETE`
3. 消费者：`INGEST` → 读文章 → 删旧 chunk → 切片 → Embedding → 入库；`DELETE` → 按 articleId 删向量
4. **手动 ack**：消费成功才确认，进程崩溃消息不丢
5. **失败重试**：消费异常重试 3 次，仍失败进入**死信队列**（可人工/定时补偿）
6. 幂等：同一文章重复消费不产生重复 chunk（按 articleId 先删后插）
7. 消息体小：只传 `{articleId, action}`，内容由消费者从库读取（避免大消息）

---

## 3. 实现方案（按点）

### 3.1 环境与依赖

```yaml
# docker-compose.yml 新增
rabbitmq:
  image: rabbitmq:management
  container_name: akb-rabbitmq
  ports: ["5672:5672", "15672:15672"]   # 15672 是管理台
```

```xml
<!-- pom.xml 新增 -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 3.2 队列设计（交换机 + 队列 + 死信）

```
exchange: rag.exchange (direct)
  ├── queue: rag.article   (绑定 rag.article, 手动 ack, 重试 3 次)
  └── DLQ: rag.article.dlq (死信队列, 重试耗尽进入)
```

`RabbitConfig`：声明交换机、主队列（`x-dead-letter-exchange` 指向死信）、死信队列、绑定。

### 3.3 消息体

```java
public record RagArticleMessage(Long articleId, String action) {}  // INGEST / DELETE
```

### 3.4 生产者（发布事件后发消息）

- `ArticleServiceImpl` 保存/删除成功后发布 `ArticleChangedEvent`
- 用 `@TransactionalEventListener(AFTER_COMMIT)`（已有设计）确保**事务提交后**才发消息，避免消费到未提交数据
- 监听器里 `rabbitTemplate.convertAndSend("rag.exchange", "rag.article", message)`

### 3.5 消费者（手动 ack + 重试）

```java
@RabbitListener(queues = "rag.article", ackMode = "MANUAL")
public void onMessage(RagArticleMessage msg, Channel channel,
                      @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
    try {
        if ("INGEST".equals(msg.action())) ragService.ingestArticle(msg.articleId());
        else ragService.removeArticle(msg.articleId());
        channel.basicAck(tag, false);        // 业务成功 → 手动确认
    } catch (Exception e) {
        throw e;                             // 失败 → 抛异常走 Spring Retry / 死信
    }
}
```

> 重试机制：`spring.rabbitmq.listener.simple.retry`（3 次，指数退避）在进程内重试；重试耗尽后 Spring 拒绝该消息（requeue=false）→ 进死信队列，消息不丢失。

### 3.6 幂等与补偿

- `ingestArticle`：先按 articleId 删旧 chunk 再入库（幂等）
- `removeArticle`：Qdrant 按 payload `articleId` 过滤删除
- 死信队列可配一个补偿消费者（记录日志 + 告警，人工排查）

---

## 4. 涉及文件

| 文件 | 改动 |
|---|---|
| `docker-compose.yml` | 新增 rabbitmq 服务 |
| `pom.xml` | 新增 spring-boot-starter-amqp |
| `application.yml` | spring.rabbitmq 配置（host/port/手动ack） |
| `config/RabbitConfig.java` | 新增：交换机/队列/死信/绑定/Jackson 转换 |
| `rag/RagArticleMessage.java` | 新增：消息体 record |
| `rag/RagProducer.java` | 新增：发消息 |
| `rag/RagConsumer.java` | 新增：消费入库/删除（手动 ack） |
| `rag/RagService.java` | 新增 `ingestArticle(Long id)` / `removeArticle(Long id)` |
| `rag/ArticleChangedEvent.java` + 监听器 | 发布事件（事务提交后） |
| `service/impl/ArticleServiceImpl.java` | 保存/删除后发布事件 |

---

## 5. 验收标准

- [ ] `docker compose up -d` 后 rabbitmq 可访问管理台（15672）
- [ ] 后台**发布**文章 → 队列出现消息 → Qdrant 新增该文章 chunk（接口秒回）
- [ ] **编辑**已发布文章 → 旧 chunk 被替换
- [ ] **下线/删除**文章 → Qdrant 移除该文章 chunk
- [ ] 消息消费失败自动重试 3 次，最终进死信队列（不丢失）
- [ ] 手动停消费者 → 发文章 → 重启消费者 → 消息被正确消费（可靠性验证）

---

## 6. 风险与备注

1. **事务边界**：必须在 AFTER_COMMIT 发消息，否则读到未提交数据
2. **ACK 语义**：手动 ack，业务成功才确认；失败抛异常 → Spring Retry 重试 → 耗尽进死信
3. **重试次数**：用 Spring Retry 控制，避免无限 requeue 打爆队列
4. **环境**：需本机 `docker compose up -d` 启动 rabbitmq（当前环境还没有）
5. **衔接**：本任务依赖「发布时触发」钩子；`RagService.ingestArticle` 为单篇入库（区别于全量 ingest）

---

## 7. 简历包装

> **基于 RabbitMQ 构建异步向量化任务队列**：文章发布与向量入库解耦，发布接口秒回；采用手动 ack + 重试 + 死信队列，消息不丢失、失败可补偿；向量入库幂等，支持文章增删改增量同步。
