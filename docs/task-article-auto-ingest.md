# 开发任务：文章发布时自动入库（RAG 增量同步）

> 关联：`rag-advanced-design.md` 第 1.4 节「入库链路」；`implementation.md` 步骤 7（手动全量入库）
> 优先级：P1（调研文档第 5 章的 P1/P3 落地点之一）
> 状态：待开发

---

## 1. 背景与目标

**现状**：文章入库是手动触发 `POST /api/admin/rag/ingest`（全量重建）。问题：

- 新发文章 / 修改文章 / 下线文章后，**向量库与文章内容不同步**（RAG 问答检索到旧内容）。
- 全量重建耗时长（每篇都要重新切片 + Embedding）。

**目标**：让向量库跟随文章生命周期**自动增量同步**：

- 发布（status 0→1）→ 自动入库该文章的 chunk
- 编辑已发布文章 → 更新该文章的 chunk（删旧建新）
- 下线（status 1→0）→ 移除该文章的 chunk
- 删除文章 → 移除该文章的 chunk
- 入库**异步**执行，不阻塞保存文章的接口；失败不影响文章保存

---

## 2. 需求（按点）

1. **发布触发**：文章 `status` 变为 1（发布）时，自动「切片 → 向量化 → 写入 Qdrant」
2. **编辑更新**：已发布文章内容变更时，删除旧 chunk 并重新入库（保证检索到最新内容）
3. **下线移除**：文章 `status` 变为 0（草稿）时，从 Qdrant 移除其全部 chunk
4. **删除移除**：删除文章时，从 Qdrant 移除其全部 chunk
5. **异步解耦**：入库在**事务提交后**异步执行，保存接口秒回；失败只记日志，不影响文章保存
6. **幂等**：同一文章重复入库不产生重复 chunk（按 articleId 先删后插）

---

## 3. 实现方案（按点）

### 3.1 触发机制：Spring 事件 + 事务提交后监听

在 `ArticleServiceImpl`（保存/删除成功后）发布事件，用 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 异步处理：

```java
// 1. 事件类
public record ArticleChangedEvent(Long articleId, String action) {
    // action: PUBLISH / UPDATE / UNPUBLISH / DELETE
}

// 2. ArticleServiceImpl 发布事件（create/update/delete 时）
applicationEventPublisher.publishEvent(new ArticleChangedEvent(id, action));

// 3. 监听器：事务提交后才执行，且异步
@Component
public class ArticleRagListener {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleChanged(ArticleChangedEvent event) {
        // 调用 RagService 增量同步
    }
}
```

> **为什么用 AFTER_COMMIT**：若在事务内同步触发入库，会读到**未提交**的数据（旧内容）；事务提交后再读才准确。这是本需求最关键的边界点。

### 3.2 RagService 新增增量方法

```java
/** 单篇入库：先按 articleId 删旧，再切片→向量→写入（幂等） */
void ingestArticle(Article article);

/** 按 articleId 移除该文章全部 chunk */
void removeArticle(Long articleId);
```

- `ingestArticle`：删除该文章旧 chunk → 按标题切片 → 逐 chunk Embedding → 批量 upsert
- `removeArticle`：Qdrant **按 payload 过滤删除**

### 3.3 VectorStoreService 新增「按文章删除」

Qdrant REST 支持按 payload 过滤删除：

```java
// POST /collections/{c}/points/delete
// body: { "filter": { "must": [ { "key": "articleId", "match": { "value": 3 } } ] } }
public void deleteByArticleId(String collection, Long articleId) {
    Map<String, Object> filter = Map.of(
        "must", List.of(Map.of("key", "articleId", "match", Map.of("value", articleId))));
    client.post().uri("/collections/{c}/points/delete", collection)
        .body(Map.of("filter", filter)).retrieve();
}
```

> 前提：chunk 入库时 **payload 必须带 `articleId`**（现有实现已带 ✓）。

### 3.4 chunk id 改为确定性

现状：`ingestAll` 用自增 seq。增量场景改为确定性 id，便于按文章定位：

```java
long id = article.getId() * 1000L + chunkIndex;   // 同一文章 chunk 唯一且连续
```

### 3.5 异步线程池

- `@EnableAsync` + 一个 `ThreadPoolTaskExecutor` Bean（核心线程 2、最大 4、队列 100）
- `@Async` 标注监听方法；入库异常 `try-catch` 记日志（可打印文章 id 便于排查）

### 3.6 全量入库保留

`POST /api/admin/rag/ingest`（全量重建）保留，作为数据不一致时的兜底重建手段。

---

## 4. 涉及文件

| 文件                                     | 改动                                                                  |
| ---------------------------------------- | --------------------------------------------------------------------- |
| `rag/ArticleChangedEvent.java`         | 新增：事件类（articleId + action）                                    |
| `rag/RagService.java`                  | 新增`ingestArticle` / `removeArticle`；改造 chunk id              |
| `rag/VectorStoreService.java`          | 新增`deleteByArticleId`（按 payload 过滤删除）                      |
| `service/impl/ArticleServiceImpl.java` | create / update / delete 后发布事件                                   |
| `config/AsyncConfig.java`              | 新增：`@EnableAsync` + 线程池 Bean                                  |
| `rag/ArticleRagListener.java`          | 新增：`@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 监听 |

前端无需改动。

---

## 5. 验收标准（Definition of Done）

- [ ] 后台**发布**一篇新文章 → Qdrant 出现该文章的 chunk（`points_count` 增加）
- [ ] **编辑**已发布文章内容 → Qdrant 中该文章旧 chunk 被替换（数量不变、内容更新）
- [ ] **下线**文章（草稿）→ Qdrant 中该文章 chunk 全部移除
- [ ] **删除**文章 → Qdrant 中该文章 chunk 全部移除
- [ ] 保存/删除文章接口**响应时间不受影响**（入库异步执行）
- [ ] 入库失败只记日志，不抛错影响文章保存
- [ ] 同一篇文章重复触发入库，Qdrant 中不产生重复 chunk

---

## 6. 风险与备注

1. **事务边界**：必须在 `AFTER_COMMIT` 触发，否则读到旧数据（本方案已处理）。
2. **异步可靠性**：线程池方案是"尽力而为"，进程重启会丢任务；生产可升级 **RabbitMQ** 消息队列（对应简历"异步向量化任务队列"，作为后续任务）。
3. **Qdrant 过滤删除**：依赖 payload 的 `articleId` 字段，入库时必须带上（现有已带，勿移除）。
4. **Embedding 慢**：单篇约 3~5 个 chunk，本机 Embedding 每篇约 10~20 秒；异步 + 后台执行，用户无感。
5. **测试**：用 curl/后台界面发文章验证；入库日志打 `[RAG]` 便于观察。

---

## 7. 后续任务（不在本次范围）

- 多路召回（BM25+向量+RRF）→ `rag-advanced-design.md` §2
- Query 改写 → §3
- RAGAS / 命中率评估 → §4
- RabbitMQ 异步任务队列（替换线程池）→ §1.4
- 父子分段切片 → §1.3
