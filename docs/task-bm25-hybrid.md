# 开发任务：BM25 + 向量双路召回（混合检索）

> 关联：`implementation.md`（RAG 检索）、`rag-advanced-design.md`（混合检索调研）
> 简历卖点：「BM25 关键词召回 + 向量语义召回双路混合检索，RRF 融合排序，兼顾精确术语与语义查询」
> 优先级：高（召回准确率 · 简历原话）

---

## 1. 背景与目标

**现状**：`RagService.search` 只有**单路向量检索**（问题 → Embedding → Qdrant 相似度 Top N）。向量检索擅长语义/意图类查询，但对**精确术语、缩写、代码标识符**（如 "HashMap 扩容"、"RRF"、"JWT"）召回偏弱，容易漏掉字面匹配的文章。

**目标**：实现**双路混合检索**：
- **BM25 关键词路**：字面匹配，擅长精确术语/短关键词
- **向量语义路**：语义匹配，擅长同义改写/意图类查询
- **RRF 融合**：两路结果按排名融合排序，取 Top K

---

## 2. 关键技术结论（调研）

- Qdrant **自托管版无原生 BM25**（`qdrant/bm25` 服务端推理是 Cloud 功能；1.19.0 源码 `Query` 枚举无 text 类型、`Document` 标注 unimplemented）
- 自托管正确做法：**客户端生成 BM25 稀疏向量**，配 Qdrant 稀疏向量 `modifier: idf`，用 Query API 的 `prefetch + fusion: rrf` 做双路融合
- 官方 API 形态（本任务依据）：
  - 建集合：`sparse_vectors: { "sparse": { "modifier": "idf" } }`
  - 稀疏向量：`{ "indices": [词id...], "values": [权重...] }`（idf 由 Qdrant 查询时修正，客户端只算 TF）
  - 混合查询：`POST /collections/{c}/points/query`，`prefetch: [dense, sparse]` + `query: { "fusion": "rrf" }`

---

## 3. 需求（按点）

1. **双路召回**：同一问题同时走 BM25 关键词检索与向量语义检索
2. **RRF 融合**：两路 Top-K 按 Reciprocal Rank Fusion 融合，返回最终 Top K
3. **中文分词**：BM25 需要分词——中文按**字符二元组（bigram）**近似分词，英文按单词，零外部依赖（生产可换 jieba/HanLP）
4. **稀疏向量**：客户端把分词结果转成 TF 稀疏向量（词 hash → 维度 id，词频 → 权重）；IDF 交给 Qdrant `modifier: idf` 处理
5. **存量迁移**：集合需从「单路 dense 向量」升级为「dense + sparse 双命名向量」，需重建集合并重新入库（幂等）
6. **降级兜底**：问题分词为空（纯标点/空串）时自动退化为纯向量检索，不报错

---

## 4. 实现方案（按点）

### 4.1 中文分词 + 稀疏向量（新增 `SparseVectorService`）

- `tokenize(text)`：小写化 → 扫描：
  - 连续中文 → 相邻两字组成 bigram（"知识库" → "知识"、"识库"）；单字则保留单字
  - 连续英文/数字 → 整词
  - 空白/标点 → 跳过
- `encode(text)`：token → 32 位 FNV-1a hash（非负）作为稀疏维度 id，统计 TF 词频，返回 `{indices[], values[]}`（indices 升序、去重）

### 4.2 集合升级为双命名向量（`VectorStoreService.ensureCollection`）

```json
PUT /collections/blog_chunks
{
  "vectors":        { "dense":  { "size": 1024, "distance": "Cosine" } },
  "sparse_vectors": { "sparse": { "modifier": "idf" } }
}
```

- 命名向量：`dense`（稠密，Ollama embedding）、`sparse`（稀疏，BM25）
- 入库 point 的 `vector` 改为 `{ "dense": [...], "sparse": { "indices": [...], "values": [...] } }`

### 4.3 混合检索（`VectorStoreService.hybridSearch`）

```json
POST /collections/blog_chunks/points/query
{
  "prefetch": [
    { "query": <dense向量>, "using": "dense",  "limit": topK*2 },
    { "query": {"indices":[...],"values":[...]}, "using": "sparse", "limit": topK*2 }
  ],
  "query": { "fusion": "rrf" },
  "limit": topK,
  "with_payload": true
}
```

- 响应解析：`resp.path("result").path("points")`（Query API 与 search API 响应结构不同）
- `RagService.search`：embedding + 稀疏向量 → `hybridSearch`；稀疏为空则退化 `search`（dense-only）

### 4.4 涉及文件

| 文件 | 改动 |
|---|---|
| `rag/SparseVectorService.java` | 新增：中文 bigram 分词 + TF 稀疏向量 |
| `rag/VectorStoreService.java` | 集合配置改双命名向量；`search` 加 `using: dense`；新增 `hybridSearch` |
| `rag/RagService.java` | 注入 SparseVectorService；入库 point 带 dense+sparse；`search` 改双路 |

---

## 5. 验收标准

- [ ] `mvn clean package` 编译通过
- [ ] 重建集合后 `ingestAll` 入库，point 同时含 dense 与 sparse 向量
- [ ] 提问命中精确术语（如文章标题里的关键词）能靠 BM25 路召回
- [ ] 提问用同义改写能靠向量路召回
- [ ] 双路结果经 RRF 融合返回 Top K，命中含 payload（引用溯源正常）
- [ ] 纯标点/空问题退化纯向量检索，不报错

---

## 6. 存量迁移（跑通时执行）

集合从单路 dense 升级为双命名向量，需重建一次：

```bash
# 1) 删除旧集合（1526 点会清空，可重新生成）
curl -X DELETE http://localhost:6333/collections/blog_chunks

# 2) 重启后端（新集合配置生效），然后触发全量入库
curl -X POST http://localhost:8080/api/rag/ingest
```

> 重新入库会重跑 1526 次 Ollama embedding + 客户端分词，耗时取决于本地模型，耐心等待。

---

## 7. 风险与备注

1. **hash 维度碰撞**：FNV-1a 32 位理论上可能两词同 id（概率极低），token 数万级可接受；要绝对无碰撞需维护词表（Redis/DB），当前不引入
2. **bigram 分词**：中文 bigram 是轻量近似，不如 jieba 精确；接口已隔离在 `SparseVectorService`，生产可平滑替换分词器
3. **BM25 近似度**：客户端只算 TF，IDF 由 Qdrant `modifier: idf` 修正，未做 k1/b 长度归一化——属"BM25 风格"关键词检索，简历措辞用"关键词检索"更稳妥
4. **后续阶段（不在本任务）**：Cross-Encoder 重排序（需独立 reranker 服务）、拒答阈值、Prompt 外置、Ragas 评测——见 `rag-advanced-design.md` 与三阶段蓝图
