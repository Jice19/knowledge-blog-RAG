# 设计：多轮对话 + 多个会话窗口

> 目标：从「单次问答」升级为「多轮对话 + 多会话窗口」的完整对话产品（类 ChatGPT / Dify 聊天界面）
> 关联：现有 `RagPage`（单次问答）、`ChatService.rewriteQuery`（多轮改写已就绪）
> 市场参考：Dify 用 `conversation_id` 贯穿 `/chat-messages` 接口；ChatGPT 用左侧会话列表 + 每条消息流式追加

---

## 1. 方案概述

核心模型：**一个会话（conversation）= 一个对话窗口，一个会话下有多条消息（message）**。

```
用户（1）──< 会话（N）──< 消息（N）
```

- 每次提问 = 向某个会话追加「用户消息 + 助手消息」
- 多轮改写 = 取该会话**最近 N 条消息**做历史（复用现有 `rewriteQuery`）
- 前端左侧会话列表 = 查询会话表；点击会话 = 加载该会话的消息

---

## 2. 数据模型（新增 2 张表）

```sql
-- 会话表
CREATE TABLE conversation (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '所属用户',
    title       VARCHAR(100) NOT NULL DEFAULT '新会话' COMMENT '会话标题（默认取首个问题）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user (user_id)
) COMMENT='会话窗口';

-- 消息表
CREATE TABLE message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT       NOT NULL COMMENT '所属会话',
    role            VARCHAR(20)  NOT NULL COMMENT 'user / assistant',
    content         TEXT         NOT NULL COMMENT '消息内容',
    references_json TEXT         DEFAULT NULL COMMENT '引用来源 JSON（assistant 消息才有）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_conversation (conversation_id)
) COMMENT='会话消息';
```

> 说明：`references_json` 存答案的引用来源（articleId/标题/章节/score），前端展示"引用溯源"，刷新后不丢。

---

## 3. 后端 API 设计

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/rag/conversations` | 会话列表（按更新时间倒序） |
| POST | `/api/rag/conversations` | 新建会话 |
| DELETE | `/api/rag/conversations/{id}` | 删除会话（级联删消息） |
| GET | `/api/rag/conversations/{id}/messages` | 加载某会话的全部消息 |
| GET | `/api/rag/conversations/{id}/ask/stream` | **在该会话内提问（SSE 流式）** |

**提问流程（核心）**：

```
POST/GET /conversations/{id}/ask/stream?q=...
 1. 加载该会话最近 5 条消息 → 组装历史
 2. Query 改写（rewriteQuery，已有）→ 独立查询
 3. 检索 Top N → 组装 prompt（保留原问题）
 4. 先存「user 消息」→ SSE 流式生成
 5. 生成完存「assistant 消息」（含 references）→ 推 references 事件 → 结束
 6. 若会话是新会话（无标题）→ 用首个问题更新标题
```

> 消息必须先落库再生成：即使生成中断，用户问题也已保存，可恢复。

---

## 4. 前端会话窗口设计

```
┌─────────────────────────────────────────────┐
│ 侧边栏（会话列表）      │  聊天区              │
│ ┌───────────────────┐ │  ┌───────────────┐  │
│ │ ➕ 新建会话        │ │  │ (历史消息气泡)  │  │
│ ├───────────────────┤ │  │ 用户：...      │  │
│ │ 💬 会话1（高亮）   │ │  │ AI：... + 引用  │  │
│ │ 💬 会话2          │ │  │ (流式输出)      │  │
│ │ 💬 会话3          │ │  │ 输入框 + 提问    │  │
│ └───────────────────┘ │  └───────────────┘  │
└─────────────────────────────────────────────┘
```

- **会话列表**：进入页面加载；点击切换；当前会话高亮；支持删除
- **聊天区**：消息气泡（用户右/助手左），Markdown 渲染（已有），引用来源可点跳文章
- **多轮**：切换会话后提问，自动带上该会话历史（后端从库读，前端无需自己拼历史）
- **新建会话**：清空聊天区，提问后自动创建会话

> 相比现在前端手动传 history，改为**后端从库读历史**——刷新页面/换设备都能续上对话。

---

## 5. 与现有代码的衔接

| 现有 | 改动 |
|---|---|
| `RagPage` 手动传 history | 改为：提问带 `conversationId`，历史由后端加载 |
| `ChatService.rewriteQuery` | 复用（输入变为从库加载的历史） |
| `RagService.buildContext` | 加 `history` 来源从会话库读取 |
| 新增 | `Conversation` / `Message` 实体 + Mapper + Service + Controller |

---

## 6. 分阶段落地步骤

| 步骤 | 内容 |
|---|---|
| **S1 后端会话** | 建表 + 实体/Mapper + 会话 CRUD API + 消息存取 |
| **S2 多轮问答** | 改造 `ask/stream`：按 conversationId 加载历史 → 改写 → 检索 → 生成 → 存消息 |
| **S3 前端会话窗** | 侧边栏会话列表 + 聊天区改造（消息气泡、切换、新建） |
| **S4 打磨** | 标题自动生成、删除会话、会话内滚动加载 |

---

## 参考

- [Dify 多轮对话上下文机制（conversation_id）](https://github.com/langgenius/dify/discussions/30767)
- [Dify 对话接口 /chat-messages 与会话管理](https://github.com/langgenius/dify/discussions/7461)
- [RAG 多轮会话管理实现参考（conversation.py）](https://github.com/agent-creativity/agentic-local-brain/blob/d1e5f846351a8433edea54053ddb5fc3158229c6/kb/query/conversation.py)
