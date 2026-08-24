# RAG 进阶方案调研：文件解析入库 / 多路召回 / Query 改写 / RAGAS 评估

> 目的：调研市面上成熟 RAG 知识库方案（Dify、FastGPT、RAGFlow、LangChain、LlamaIndex 等）在「多类型文件解析入库、多路召回、Query 改写、效果评估」四个方向的做法，为项目 Phase 2 进阶（2.5 之后）提供设计依据。
> 关联：本文与 `方案计划书.md` 第 11 章（Phase 2 前瞻设计）、`implementation.md` 步骤 7~9 呼应。

---

## 1. 多类型文件上传解析 → 向量化入库

### 1.1 市场主流方案对比

| 平台                             | 支持格式                                   | 解析策略                                          | 入库机制                                            |
| -------------------------------- | ------------------------------------------ | ------------------------------------------------- | --------------------------------------------------- |
| **Dify**                   | PDF / Word / Excel / Markdown / TXT / HTML | 内置解析器，扫描件可开 OCR                        | 上传 → 解析 → 切片 → Embedding → 入库（异步）   |
| **FastGPT**                | 多格式                                     | 内置 + 可扩展解析                                 | 文件 → 分段（支持**父子分段**）→ 向量化入库 |
| **RAGFlow**                | PDF / Word / PPT / Excel / MD              | **深度文档理解**（版面/表格/公式，DeepDoc） | 模板化切片，质量高                                  |
| **LangChain / LlamaIndex** | 海量格式                                   | Loader 生态（unstructured、PyPDF、Docling…）     | 完全自定义 pipeline                                 |

> 选型要点：成熟平台（Dify/FastGPT/RAGFlow）把"解析→切片→入库"做成了开箱即用的异步 pipeline；自研则要用我们的 `DocumentParser` 抽象 + 异步队列自己拼。

### 1.2 解析策略要点（自研参考）

1. **格式分发**：`DocumentParser` 接口按扩展名路由（`.md/.txt → 原生；`.html → Jsoup；`.docx → POI；`.pdf → MinerU`），统一归一为「Markdown 正文 + 元数据」。
2. **PDF 用 MinerU**：版面分析 + 表格识别 + 公式(LaTeX) + OCR，输出结构化 Markdown（详见 `方案计划书.md` 11.4）。
3. **OCR 兜底**：扫描版 PDF 需 OCR（PaddleOCR / Tesseract），学习项目可先只支持文本型。

### 1.3 切片策略（市场最佳实践）

| 策略                               | 做法                                      | 优点                                               | 采用方                    |
| ---------------------------------- | ----------------------------------------- | -------------------------------------------------- | ------------------------- |
| **结构感知切片**             | 按标题层级切（`##` 边界），保留标题路径 | 语义完整、可溯源                                   | Dify / RAGFlow / 我们已有 |
| **父子分段**（Parent-Child） | 父块存整段上下文，子块（小段）做向量      | **召回率↑**：小段向量更精确，父块提供上下文 | FastGPT / LlamaIndex      |
| **重叠（overlap）**          | 相邻块重叠 50~100 字                      | 避免语义被切断                                     | 通用                      |
| **元数据**                   | 来源、标题路径、页码、时间                | 引用溯源、过滤                                     | 通用                      |

> 父子分段是我们下一步最值得做的切片优化——能显著提升"大段落文章"的召回质量。

### 1.4 入库链路（生产级）

```
上传文件
  → 异步任务队列（RabbitMQ，解耦，避免拖慢上传接口）
  → DocumentParser 解析 → 归一为 Markdown
  → 结构感知切片（+父子分段）→ 生成 chunk + 元数据
  → Embedding 向量化 → 存入 Qdrant（带 payload）
  → 增量更新：文章变更 → 删旧 chunk → 重新入库（幂等）
```

> 对应我们简历里的「RabbitMQ 异步向量化任务队列」，也是 [LightRAG 文件处理流水线](https://github.com/HKUDS/LightRAG/blob/e98f3b10/docs/FileProcessingPipeline.md) 的简化版。

---

## 2. 多路召回（Hybrid Retrieval）

### 2.1 为什么需要多路召回

- **纯向量检索**：语义理解好，但**专有名词/精确关键词**（如 "Spring Boot 3.4"、"JWT"）可能召回差；
- **纯关键词（BM25）**：精确匹配好，但**同义改写**（"缓存穿透" vs "缓存未命中"）召回差；
- 两者互补，**双路召回是当前生产 RAG 的主流标配**。

### 2.2 完整链路

```
问题
  → 路1：BM25 关键词召回（精确匹配）
  → 路2：向量语义召回（Embedding 相似度）
  → 融合（RRF / 加权）→ Top 20
  → Rerank 重排序（精排模型）→ Top 3~5
  → 送 LLM 生成
```

### 2.3 融合策略对比

| 策略                                    | 做法                              | 优点             | 缺点           |
| --------------------------------------- | --------------------------------- | ---------------- | -------------- |
| **RRF**（Reciprocal Rank Fusion） | 按排名倒数加权`1/(k+rank)` 合并 | 无需调权重、鲁棒 | 忽略分数信息   |
| **线性加权**                      | `score = w1×向量 + w2×BM25`   | 直观可调         | 权重需要实验调 |
| **Rerank 精排**                   | 召回 Top N 后用模型重排           | 效果提升最明显   | 多一步模型调用 |

> 参考：[Elasticsearch 官方 Hybrid Search（RRF）](https://www.elastic.co/search-labs/tutorials/search-tutorial/vector-search/hybrid-search)、[项目实践 RRF 决策记录](https://github.com/Brainwires/project-rag/blob/main/docs/adr/002-hybrid-search-with-rrf.md)、[混合检索 Rerank 调优](https://developer.aliyun.com/article/1755769)

### 2.4 重排序模型

- **bge-reranker-v2-m3**（BAAI，多语言、中文好、[HuggingFace](https://huggingface.co/BAAI/bge-reranker-v2-m3)）——开源首选，**Ollama 可本地跑**
- Cohere Rerank（API，质量高但收费）

### 2.5 我们项目的落地路径

- **方案 A（同库混合，推荐）**：Qdrant 2.5+ 内置全文检索（BM25），向量 + BM25 同一个库做混合检索，再加 RRF 融合
- **方案 B**：引入 Elasticsearch 做 BM25（黑马学过 ES 的话最顺）
- 两种方案都建议叠加 `bge-reranker-v2-m3` 做精排

---

## 3. Query 改写（Query Rewriting）

### 3.1 为什么需要

多轮对话的**省略/指代**问题：用户先问"Spring Boot 怎么配 Redis？"，再问"那它和 RabbitMQ 呢？"——第二句直接检索会失败（缺少"Spring Boot"上下文）。这是知识库问答在生产中最大的痛点之一。

### 3.2 主流方法

| 方法                           | 做法                                                            | 适用           |
| ------------------------------ | --------------------------------------------------------------- | -------------- |
| **多轮改写**（最常用）   | 把「历史对话 + 当前问题」交给 LLM，改写成**独立完整查询** | 多轮对话       |
| **HyDE**（假设文档嵌入） | 先用 LLM 生成"假设答案"，再拿答案去检索                         | 单轮、提升召回 |
| **查询扩展**             | 补充同义词 / 相关术语                                           | 专有名词       |
| **子查询拆分**           | 复杂问题拆成多个子问题分别检索                                  | 复合问题       |
| **Step-back**            | 先改写为更通用的问题再检索                                      | 概念性问题     |

### 3.3 市场落地

- Dify / RAGFlow：内置对话历史改写
- LangChain：`MultiQueryRetriever`、HyDE、`CondenseQuestionChatHistory` 等现成组件
- 参考：[Query Rewrite 练习与原理](https://github.com/kobejiasuoer/awesome-agent-engineering/blob/main/rag-lessons/07_query_rewrite/exercise.md)

### 3.4 我们项目的落地路径

- **第一步（简单版）**：后端维护对话历史（最近 3~5 轮），提问时用 qwen3 把「历史 + 当前问题」改写成独立查询，再走检索
- 第二步（进阶）：HyDE —— 先让模型生成假设答案再检索，进一步提高召回

---

## 4. RAGAS 效果评估

### 4.1 为什么需要

- RAG 效果好坏**不能靠感觉**，要有量化指标（简历里"准确率提升 38%"必须有评测支撑）
- 改切片 / 召回 / 模型参数后，用同一评测集**对比分数**，判断改好改坏

### 4.2 RAGAS 核心指标（LLM-as-judge）

| 指标                                        | 衡量什么                 | 说明             |
| ------------------------------------------- | ------------------------ | ---------------- |
| **Faithfulness（忠实度）**            | 答案是否忠于检索内容     | 防幻觉的关键指标 |
| **Answer Relevancy（答案相关性）**    | 答案是否切题             |                  |
| **Context Precision（上下文精确率）** | 检索结果排序是否合理     |                  |
| **Context Recall（上下文召回率）**    | 检索是否覆盖答案所需信息 | 召回质量的标尺   |

> 参考：[RAGAS 官方评估文档](https://docs.ragas.io/en/v0.1.21/getstarted/evaluation.html)、[RAGAS 核心指标解析](https://cloud.baidu.com/article/3373291)

### 4.3 使用流程

1. **准备评测集**：手写 20~30 条 `(question, ground_truth)` 问答对（覆盖各分类）
2. **跑 RAGAS**：RAGAS 用 LLM 当裁判打分（可用本地 Ollama 当 judge，省 API 费）
3. **迭代**：改切片 / 召回 / 模型 → 重跑 → 对比分数（记录成表格，面试可展示）

### 4.4 我们项目的落地路径

- **轻量自研**：实现「命中率 / MRR」——把每条问题的期望命中文章标注好，看检索是否召回（不依赖 RAGAS，跑在我们 Java 侧）
- **进阶**：引入 RAGAS（Python，Ollama 做 judge）跑完整四项指标

---

## 5. 综合落地建议（按优先级）

| 优先级       | 方向                       | 落地点                       | 对应简历卖点       |
| ------------ | -------------------------- | ---------------------------- | ------------------ |
| **P1** | 父子分段切片               | 切片服务升级                 | 召回准确率提升     |
| **P1** | 多路召回（BM25+向量）+ RRF | Qdrant 混合检索              | BM25+向量双路召回  |
| **P2** | Rerank 精排                | bge-reranker-v2-m3（Ollama） | 重排序提升准确率   |
| **P2** | Query 多轮改写             | LLM 改写历史对话             | 多轮对话优化       |
| **P2** | 效果评估                   | 评测集 + 命中率/MRR + RAGAS  | 数据驱动优化       |
| **P3** | 发布时自动入库             | 异步队列触发                 | 异步向量化任务队列 |

> 建议顺序：**先做评测基线（P2）再做优化（P1）**——没有评测，优化就无从量化。

---

## 参考来源

- [大模型知识库（RAG）构建实践：FastGPT / Dify / RAGFlow 如何选型](https://blog.csdn.net/weixin_37647148/article/details/158457817)
- [MinerU 如何整理文档进 RAG 流程](https://cloud.tencent.cn/developer/article/2700453)
- [LightRAG 文件处理流水线](https://github.com/HKUDS/LightRAG/blob/e98f3b10/docs/FileProcessingPipeline.md)
- [Elasticsearch Hybrid Search（RRF）](https://www.elastic.co/search-labs/tutorials/search-tutorial/vector-search/hybrid-search)
- [项目实践：混合检索 RRF 决策记录](https://github.com/Brainwires/project-rag/blob/main/docs/adr/002-hybrid-search-with-rrf.md)
- [混合检索 Rerank 调优全流程](https://developer.aliyun.com/article/1755769)
- [bge-reranker-v2-m3（BAAI 重排序模型）](https://huggingface.co/BAAI/bge-reranker-v2-m3)
- [Query Rewrite 原理与练习](https://github.com/kobejiasuoer/awesome-agent-engineering/blob/main/rag-lessons/07_query_rewrite/exercise.md)
- [RAGAS 官方评估文档](https://docs.ragas.io/en/v0.1.21/getstarted/evaluation.html)
- [RAGAS 核心评估指标解析](https://cloud.baidu.com/article/3373291)
