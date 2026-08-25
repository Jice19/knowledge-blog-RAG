# RAG 轻量评测

量化 RAG 的三个核心指标：**检索命中率（Hit@K）、忠实度（Faithfulness）、相关性（Relevance）**。设计文档见 `docs/task-rag-eval.md`。

## 前置条件

1. 后端已启动：`mvn clean spring-boot:run`
2. Ollama 已启动，且已拉取 `qwen3:0.6b`（裁判用）与 embedding 模型

## 第一步：导入真实文章

`content/articles/` 下已备好 26 篇真实技术文章（Java / Redis / RabbitMQ / Spring / MySQL / 网络 / RAG）。

```bash
# 登录 admin/admin123 → 建分类 → 逐篇发布（status=1，自动触发异步向量化）
python3 scripts/import_articles.py
```

脚本会导出 `content/imported_articles.json`（标题 → 文章 id 映射），供评测集解析。

> 导入后需等待向量化队列消费完成（发布接口秒回，入库是异步的），稍等片刻再跑评测。

## 第二步：跑评测

评测集 `evaluation/golden_set.json` 已内置 30 条问答，通过 `expected_article_title` 关联到上面的文章。

```bash
python3 evaluation/run_eval.py
```

输出：逐条结果 + 汇总（Hit@1 / Hit@3 / 平均忠实度 / 平均相关性），并写入 `evaluation/report.md`。

## 指标解读

| 指标 | 好 | 说明 |
|---|---|---|
| Hit@3 | ≥ 0.8 | 检索召回：正确文章进 Top3 的比例（最可信的硬指标） |
| Faithfulness | ≥ 0.8 | 忠实度：答案被资料支撑的程度（反幻觉） |
| Relevance | ≥ 4 | 相关性：与标准答案吻合度（1~5） |

## 如何扩充

- 增文章：往 `content/articles/` 加 md 文件（frontmatter 格式照抄现有文件），重跑导入脚本
- 增评测：往 `golden_set.json` 加条目，`expected_article_title` 填对应文章标题即可

## 注意

- 裁判是本地 `qwen3:0.6b`，忠实度/相关性仅作参考；**Hit@K 是无 LLM 的硬指标**，最能写进简历
- `expected_article_title` 必须与文章标题完全一致，脚本才能解析到 id
