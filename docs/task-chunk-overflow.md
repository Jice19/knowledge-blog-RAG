# 开发任务：切片超长兜底（按句子二次切分 + overlap 重叠）

> 关联：`implementation.md`（切片策略）、`rag-advanced-design.md`（结构化切片）
> 简历卖点：「结构化切片：标题级切片 + 超长按句二次切分 + 重叠窗口，兼顾语义完整性与上下文连贯性」
> 优先级：中（RAG 健壮性优化）

---

## 1. 背景与目标

**现状**：`ChunkService` 只按 `##` 标题切片，有两个缺陷：
- 某篇正文没有 `##` 标题时，**整篇作为一块**，文章过长会超出 Embedding 模型输入上限（mxbai-embed-large 约 512 token）；
- 单个标题块内容过长时，向量化的信息密度差、检索精度下降。

**目标**：在标题切片基础上增加**超长兜底**——标题块超过阈值时按句子二次切分，相邻片段保留 overlap 重叠，避免语义在边界断裂、上下文丢失；子片继承原标题，引用溯源不受影响。

---

## 2. 需求（按点）

1. 标题切出的块超过 `chunk-max-chars`（默认 1000 字符）时，**按句子二次切分**（中文/英文句末标点 + 换行），不硬切单词或句子中间
2. 相邻二次切分子片之间保留 `chunk-overlap-chars`（默认 100 字符）的**重叠文本**，保证被切开的上下文在两侧都能被检索到
3. 二次切分产生的子片**继承原标题 heading**，引用溯源仍定位到具体章节
4. 参数**配置化**：走 `rag.*` 配置项（与 ollama-url 等一致），可调不用改代码
5. 兼容现有行为：标题块不超过阈值时**不切**；无 `##` 标题的整篇也走超长兜底
6. 单个句子本身超过阈值（无句末标点的超长文本）时，**按字符硬切**兜底（同样带 overlap），保证任何情况都能切到阈值内

---

## 3. 实现方案（按点）

### 3.1 配置项

`RagProperties` 新增两个字段（默认值兜底，yml 可覆盖）：

```yaml
# application.yml → rag:
chunk-max-chars: 1000      # 标题块超过该字符数触发二次切分
chunk-overlap-chars: 100   # 相邻片段重叠字符数
```

### 3.2 切割流程（ChunkService 改造）

```
标题扫描（原有逻辑，按 "## " 边界）
  └─ 每个标题块 → splitOverflow(text, heading)
        ├─ text.length <= max        → 直接成片
        └─ text.length >  max        → 按句子贪心打包成多个窗口
               └─ 新窗口开头 = 上一窗口末尾 overlap 字符（重叠）
               └─ 单句 > max（无标点长文本）→ 按字符硬切（带 overlap）
```

要点：
- **句子切分**：正则 `[。！？!?；;\n]+` 作为边界，保留分隔符（"第一句。第二句。" 不会被切成 "第一句。" + "第二句"）
- **贪心打包**：句子依次塞进当前窗口，塞不下则收尾成片，下一窗口从上一片末尾取 overlap 字符开头
- **heading 继承**：所有子片 heading 与标题块一致
- **窗口上限**：每片 ≤ max + overlap（overlap 是借自上一片的文本，属预期行为）

### 3.3 涉及文件

| 文件 | 改动 |
|---|---|
| `config/RagProperties.java` | 新增 `chunkMaxChars` / `chunkOverlapChars`（带默认值） |
| `resources/application.yml` | rag 段新增 `chunk-max-chars` / `chunk-overlap-chars` |
| `rag/ChunkService.java` | 新增 `splitOverflow` / `splitSentences` / `hardSplit` 三个方法，标题块输出改为 `splitOverflow` 结果 |

---

## 4. 验收标准

- [ ] 有 `##` 标题且每块 < max → 行为与原来完全一致（不切）
- [ ] 无 `##` 标题的超长文章 → 被按句子切成多片，每片 ≤ max + overlap
- [ ] 相邻子片首尾有 overlap 重叠文本
- [ ] 所有子片 heading 与标题块一致（引用溯源不丢章节）
- [ ] 纯长文本（无句末标点）→ 按字符硬切，不产生超长片
- [ ] `mvn clean package` 编译通过

---

## 5. 风险与备注

1. **存量数据不受影响**：切片只影响新入库；如需按新策略重切，需重新全量 ingest（Qdrant 幂等 upsert）
2. **句子切分是正则近似**：代码块内的换行/句号也会被切（与现有"代码块 `#` 误判"是同类局限，生产可用 flexmark 解析 AST 解决，见 `ChunkService` 注释）
3. **overlap 借字**：窗口上限是 max + overlap，属设计内行为，避免 overlap 前缀单独成片
4. **Embedding 长度**：1000 字符 × 中文≈1 token/字，加上 overlap 仍在 512 token 模型上限内（若换更大阈值需重新评估）
