# 开发任务：RAG 轻量评测（检索命中率 + 忠实度 + 相关性）

> 关联：`task-bm25-hybrid.md`（被评测对象）、三阶段蓝图「第三阶段：交付级质量保障」
> 简历卖点：「搭建离线评测集与量化指标，检索命中率 / 答案忠实度可量化，指导召回策略迭代」
> 优先级：中（简历含金量 · 让"准确率"从拍脑袋变可复现）

---

## 1. 背景与目标

**现状**：RAG 召回效果靠"感觉"，简历里不敢写任何量化数字（因为没测过）。

**目标**：用最小成本建一套**离线评测**，让三个问题可量化回答：
1. 检索对不对（召回的文章是不是对的）
2. 答得老不老实（有没有瞎编）
3. 答得准不准（和标准答案比）

---

## 2. 指标定义（按点）

| 指标 | 含义 | 计算方式 | 依赖 LLM |
|---|---|---|---|
| **Hit@K 检索命中率** | 正确来源文章是否进入 Top-K 引用 | 标准答案标注的正确文章 id 出现在引用前 K 条 → 命中 | ❌ 纯客观 |
| **Faithfulness 忠实度** | 答案每句是否被检索资料支撑 | LLM 裁判：支持=1 / 部分=0.5 / 不支持=0，取平均 | ✅ LLM 裁判 |
| **Relevance 相关性** | 答案与标准答案的吻合度 | LLM 裁判 1~5 分，取平均 | ✅ LLM 裁判 |

> 三指标对应 RAG 三阶段：检索质量（Hit@K）、幻觉控制（Faithfulness）、答案质量（Relevance）。

---

## 3. 实现方案（按点）

1. **评测集 `golden_set.json`**：人工标注问答对，字段 `question / reference_answer / expected_article_title`（正确来源文章标题，脚本按标题解析成 id；也兼容显式 `expected_article_id`）
2. **评测脚本 `run_eval.py`**（纯标准库，零依赖，Python3）：
   - 对每个问题调 `GET /api/rag/ask?q=&topK=` → 拿 `answer` + `references`（算 Hit@K）
   - 调 `GET /api/rag/search?q=&topK=` → 拿召回 chunk 原文 `payload.text`（给裁判当"资料"）
   - 调 Ollama `/api/chat` 当裁判，分别输出忠实度 / 相关性
3. **`--list-articles`** 模式：拉 `GET /api/articles` 打印文章 id/标题/摘要，辅助造评测集
4. **输出**：控制台逐条结果 + 汇总（Hit@1 / Hit@3 / 平均忠实度 / 平均相关性），写 `report.md`

---

## 4. 涉及文件

| 文件 | 说明 |
|---|---|
| `content/articles/*.md` | 26 篇真实技术文章（评测集的知识来源） |
| `scripts/import_articles.py` | 批量导入文章并导出标题→id 映射 |
| `evaluation/golden_set.json` | 评测集（30 条问答，按标题对应） |
| `evaluation/run_eval.py` | 评测脚本 |
| `evaluation/README.md` | 使用方法 + 造评测集步骤 |

---

## 5. 验收标准

- [ ] 后端 + Ollama 启动后，`python3 evaluation/run_eval.py` 能跑通并输出三项指标
- [ ] `--list-articles` 能列出文章 id/标题，可据此造评测集
- [ ] 评测集 ≥ 30 条后，得出可写进简历的 Hit@K 与忠实度数字

---

## 6. 风险与备注（诚实）

1. **裁判本身是本地小模型** `qwen3:0.6b`，裁判判断可能不准 → **Hit@K 是最可信的硬指标**，忠实度/相关性仅作参考
2. **ask 与 search 的召回文本有轻微差异**：ask 内部做了 Query 改写，search 未改写；用 search 的 topK 文本当"资料"是近似，轻量评测可接受
3. **评测集质量是关键**：标注错了标准答案，指标再好看也是假的；务必人工核对 expected_article_id
4. **不引入 Ragas**：Ragas 是 Python 生态且依赖较重；本方案三指标已覆盖 RAG 核心，且零依赖、可解释
