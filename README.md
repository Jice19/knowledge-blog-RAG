# AI 智识博客 · 知识库问答平台

面向个人技术博客的 AI 知识库问答平台：基于 RAG（检索增强生成）架构，用户用自然语言提问，系统从文章向量库检索相关内容并由大模型整合作答，支持引用溯源与多轮会话；文章发布与向量入库通过 RabbitMQ 异步解耦，保证数据一致与接口可靠。

## 功能特性

- **博客基础**：文章 / 分类 / 标签管理、Markdown 发布、分页列表与热门排行
- **RAG 问答**：文本切片（按标题切片 + 超长按句二次切分 + overlap）→ Embedding → Qdrant 向量检索 → 大模型整合答案；SSE 流式输出、引用溯源、多轮会话
- **双路召回**：BM25 关键词 + 向量语义双路混合检索，RRF 融合排序
- **异步向量化**：RabbitMQ 解耦发布与向量入库，发布接口秒回；手动 ack + 失败重试 + 死信队列兜底；入库幂等，支持增删改增量同步
- **后端工程化**：JWT 无状态鉴权（Redis 黑名单登出）、BCrypt 加密、全局异常拦截、统一响应体、Redis 缓存、分层 RESTful 架构
- **离线评测**：黄金问答集 + LLM-as-Judge 评测脚本，量化检索命中率 / 忠实度 / 相关性

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.4 · MyBatis-Plus · MySQL 8 · Redis · RabbitMQ |
| AI | Ollama（qwen3 生成 + mxbai-embed-large 向量）· Qdrant 向量库 · 客户端 BM25 稀疏向量 |
| 前端 | React 18 · TypeScript · Vite · Tailwind CSS |
| 基础设施 | Docker Compose（MySQL / Redis / Qdrant / RabbitMQ） |

## 快速开始

前置：Docker、JDK 17+、Node 18+、Ollama（已拉取 `qwen3:0.6b` 与 `mxbai-embed-large`）。

```bash
# 1. 启动中间件（MySQL 3307 / Redis 6379 / Qdrant 6333 / RabbitMQ 5672+15672）
docker compose up -d

# 2. 启动后端（首次启动自动建表并创建管理员 admin/admin123，端口 8080）
cd backend && mvn clean spring-boot:run

# 3. 启动前端
cd frontend && npm install && npm run dev

# 4.（可选）导入知识库真实文章并触发异步向量化
python3 scripts/import_articles.py
```

- 前端：http://localhost:5173
- 后端 API：http://localhost:8080
- RabbitMQ 管理台：http://localhost:15672（guest / guest）

## 配置说明

- 后端端口 `8080`；MySQL 宿主机端口 `3307`（root / root，库 `ai_knowledge_blog`）
- RAG 配置在 `backend/src/main/resources/application.yml` 的 `rag` 段：Ollama / Qdrant 地址、模型名、切片阈值（`chunk-max-chars` / `chunk-overlap-chars`）等
- Qdrant 集合 `blog_chunks`：`dense`（1024 维 Cosine）+ `sparse`（BM25，idf 修正）；从旧版单向量集合升级需按 `docs/task-bm25-hybrid.md` 重建迁移

## 知识库与评测

| 目录/文件 | 说明 |
|---|---|
| `content/articles/` | 26 篇真实技术文章（frontmatter：title / summary / category） |
| `scripts/import_articles.py` | 批量导入文章（登录 → 建分类 → 发布 → 导出标题映射） |
| `evaluation/golden_set.json` | 30 条黄金问答评测集 |
| `evaluation/run_eval.py` | 离线评测脚本（Hit@K / 忠实度 / 相关性） |
| `evaluation/README.md` | 评测使用说明 |

```bash
python3 scripts/import_articles.py   # 导入文章（自动异步向量化）
python3 evaluation/run_eval.py       # 跑评测，输出指标并写入 evaluation/report.md
```

## 文档索引

- [方案计划书](./方案计划书.md)：项目整体设计（选型、目录、数据库、API、里程碑）
- [implementation.md](./docs/implementation.md)：RAG 实现全流程（切片 → 向量 → 检索 → 问答）
- [database.md](./docs/database.md) / [auth.md](./docs/auth.md)：数据库设计与鉴权设计
- [rag-advanced-design.md](./docs/rag-advanced-design.md)：RAG 进阶调研（混合检索 / 重排 / 评测）
- `docs/task-*.md`：各功能任务文档（异步向量化、多轮会话、切片优化、BM25 双路召回、轻量评测）
