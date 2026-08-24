# 实现文档（Implementation Log）

> 本文件是项目的**实现日志**，记录每一步做了什么、为什么这么做、产生了哪些文件。
> 所有项目文档统一放在 `docs/` 目录下。

## 文档规范

1. **命名**：英文小写 + 连字符（`implementation.md`、`database.md`、`auth.md`、`api.md`）
2. **每个步骤记录**：需求（按点列出）+ 实现方案（按点列出）+ 产出 + 关键决策
3. **拆分原则**：`implementation.md` 记录「过程与决策」；表结构、鉴权、接口等专题另开独立文档
4. **提交约定**：每一步只 `git commit`，由作者自行 `git push`
5. **目的**：方便日后回顾 + 面试复盘

## 文档索引

| 文档 | 内容 | 状态 |
|---|---|---|
| implementation.md | 实现日志（本文件） | 进行中 |
| database.md | 数据库表结构 | 步骤 2 创建 |
| auth.md | 鉴权方案（JWT） | 步骤 2 创建 |
| api.md | 接口文档 | 待创建 |

---

## 步骤回顾

### 步骤 0 · 项目初始化与方案设计
- **产出**：`README.md`、`方案计划书.md`（Phase 1 博客 MVP + Phase 2 内容导入解析设计）
- **关键决策**：技术栈定为 Java 17 + SpringBoot 3 + MyBatis-Plus + MySQL 8 + Redis + RabbitMQ + Milvus + React 18 + TS；PDF 解析选 MinerU

### 步骤 1 · 前端界面
- **产出**：`frontend/` 全套（前台 3 页 + 后台 6 页），mock 数据 + DataContext 内存态 CRUD
- **关键决策**：Tailwind 自定义设计；`.npmrc` 走 npmmirror 镜像（npmjs 源超时）

---

## 步骤 2 · 数据库表设计 + 用户/管理员 JWT 鉴权

### 2.1 需求（按点）

1. 建立 **5 张数据库表**：`user` / `category` / `tag` / `article` / `article_tag`，含索引、时间戳默认值、幂等建表（`IF NOT EXISTS`）
2. 后端 **SpringBoot 骨架**可启动：Java 17 + SpringBoot 3 + MyBatis-Plus + MySQL + Redis
3. **统一返回体** `Result` + **全局异常**处理，所有接口返回 `{code, message, data}`
4. **管理员登录** `POST /api/auth/login`：用户名密码 → 校验 → 返回 JWT
5. 密码 **BCrypt** 加密存储（不存明文）
6. **JWT 鉴权**：拦截器校验 `Authorization: Bearer <token>`，通过 `ThreadLocal` 存当前用户
7. **获取当前用户** `GET /api/auth/me`
8. **退出登录** `POST /api/auth/logout`：token 写入 Redis 黑名单
9. **首次启动**自动建表 + 种子管理员账号（`admin` / `admin123`）

### 2.2 实现方案（按点）

1. **目录结构**：`backend/` 按分层组织（common / config / security / controller / service / mapper / entity / dto / vo）
2. **建表**：`schema.sql` 用 `CREATE TABLE IF NOT EXISTS`（幂等），`spring.sql.init` 启动时自动执行
3. **依赖选型**：MyBatis-Plus 用 `mybatis-plus-spring-boot3-starter`（Boot3 专用，非 boot2 的 starter）；JWT 用 `jjwt 0.12`；Maven 配 **Aliyun 镜像**（国内网络）
4. **实体映射**：5 个 entity 用 Lombok `@Data`，时间字段 `LocalDateTime`，依赖 DB 默认值填充
5. **统一返回**：`Result<T>` + `ResultCode` 枚举 + `BusinessException` + `@RestControllerAdvice` 全局异常
6. **鉴权链路**：`JwtUtil`（HS256 签发/解析）→ `JwtAuthInterceptor`（黑名单校验 + 解析 token + 写 `UserContext`）→ `WebMvcConfig` 注册拦截器与 CORS
7. **登录**：`AuthService` 查库 + BCrypt 校验 + 签发 token；`/api/admin/**`、`/api/auth/me`、`/api/auth/logout` 拦截，`/api/auth/login` 放行
8. **退出**：token 写入 Redis 黑名单 `jwt:blacklist:<token>`，TTL = token 剩余有效期
9. **种子账号**：`CommandLineRunner` 首次启动时若 user 表为空则创建 `admin/admin123`

### 2.3 产出

- `backend/`（SpringBoot 骨架，见上）
- `docker-compose.yml`（MySQL 8 + Redis 7）
- `docs/database.md`（表结构）、`docs/auth.md`（鉴权设计）

### 2.4 完成情况

- ✅ 后端可编译打包：`mvn package -DskipTests` → BUILD SUCCESS
- ✅ 5 张表 `schema.sql`（启动自动建表）
- ✅ JWT 登录 / 鉴权 / 退出（Redis 黑名单）
- ✅ 默认管理员账号 `admin / admin123`
- ✅ 环境：Docker 运行 MySQL(3307) + Redis(6379)，避开本机 MySQL(3306)

---

## 步骤 3 · 前后端联调 + 用户增删改查

### 3.1 需求（按点）

1. 前端**接入真实后端接口**，替换 mock 数据，打通登录鉴权链路（登录 → 拿 JWT → 携带鉴权）
2. 后端提供**用户管理接口**：分页查询、新增、修改、删除
3. 前端新增**「用户管理」页面**，对接上述接口
4. 前端 **Axios 统一封装**：自动携带 JWT、统一解包 `Result`、`401` 自动跳登录

### 3.2 实现方案（按点）

1. 前端 `api/http.ts`：Axios 实例 + 请求拦截器（带 `Authorization: Bearer`）+ 响应拦截器（解包 Result + 401 跳转）
2. 前端 `api/auth.ts`：`login` / `me` / `logout` 对接后端
3. 后端 `UserController`（`/api/admin/users`）：`GET` 分页 / `POST` 新增 / `PUT /{id}` 修改 / `DELETE /{id}` 删除
4. 后端 `UserService` + `UserDTO` + `UserVO` + `PageResult<T>`
5. 前端 `UserManagePage`：表格 + 新增/编辑弹窗 + 删除 + 分页
6. 登录态：token 存 localStorage，当前用户信息存 localStorage；`AdminLayout` 退出时调 `/api/auth/logout` 拉黑 token
7. 删除保护：不能删除当前登录用户自己

### 3.3 产出

- 后端 user 模块（Controller / Service / DTO / VO / PageResult）
- 前端 `src/api/`（http.ts、auth.ts、user.ts）
- 前端 `UserManagePage` + 路由 + 侧边栏导航
- 更新 `LoginPage` / `AdminLayout` / `Navbar` 接真实接口
- 统一登录：`/login` 单入口，登录后按角色跳转（ADMIN → /admin，USER → /）

### 3.4 完成情况

- ✅ 后端用户 CRUD 可编译（BUILD SUCCESS）
- ✅ 前端构建 + 类型检查通过
- ✅ 统一登录 + 角色跳转
- ✅ 用户管理页（分页 / 新增 / 编辑 / 删除 / 搜索）

---

## 步骤 4 · 文章 / 分类 / 标签 CRUD（M2）

### 4.1 需求（按点）

1. 后端**分类**：公开列表 + 管理端增删改
2. 后端**标签**：公开列表 + 管理端增删改
3. 后端**文章**：发布/草稿、分页、前台公开列表（仅已发布）+ 后台管理列表（全部）、详情浏览量自增、标签关联
4. 前端**博客前台**接真实数据（首页 / 详情 / 分类页 / Navbar）
5. 前端**后台管理**接真实数据（文章 / 分类 / 标签 / 仪表盘）
6. 移除 `DataContext` mock 数据

### 4.2 实现方案（按点）

1. 分类/标签：返回实体（无敏感字段）；管理接口统一走 `/api/admin/**`（已有拦截器保护）
2. 文章：`ArticleVO` 聚合分类名 / 标签列表 / 作者名；`ArticleDTO` 含 `tagIds`
3. 文章标签：创建/更新时重建 `article_tag` 关联；删除标签/文章时清理关联
4. 前台文章接口只返回 `status=1`；后台接口返回全部状态
5. 详情浏览量用 `setSql("view_count = view_count + 1")` 原子自增
6. 前端新建 `api/category.ts` / `api/tag.ts` / `api/article.ts`；页面用 `useEffect` 拉取 + loading 状态
7. 移除 `DataContext` + `mock.ts`，`main.tsx` 去掉 DataProvider

### 4.3 产出

- 后端 category / tag / article 模块（DTO / VO / Service / Controller）
- 前端 api 模块 + 全部页面接真实数据
- 移除 mock 数据

### 4.4 完成情况

- ✅ 后端文章/分类/标签 CRUD 可编译（BUILD SUCCESS）
- ✅ 前端构建 + 类型检查通过
- ✅ 前台/后台全部接真实数据，移除 mock
- ✅ 种子数据：`DataInitializer` 首次启动写入 6 分类 / 10 标签 / 6 文章（category 表为空时执行，幂等）

---

## 步骤 5 · 性能优化：修复 N+1 + 分类/标签缓存

### 5.1 需求（按点）

1. 修复文章列表的 **N+1 查询**（原实现每篇文章单独查分类 / 标签 / 作者）
2. 分类 / 标签列表走 **Redis 缓存**，变更时失效

### 5.2 实现方案（按点）

1. 文章 VO 改为「**批量查询 + 内存组装**」：`batchToVO` 一次性查出所有分类 / 作者 / 标签，再内存映射
2. 分类 / 标签 `listAll` 用 **Cache-Aside**：先查 Redis，未命中查库回填（TTL 1h）；create / update / delete 时 `evictCache()` 删缓存

### 5.3 完成情况

- ✅ 后端 BUILD SUCCESS
- ✅ 文章列表查询次数：N+1 → 固定 4 次（文章 + 分类 + 作者 + 标签）
- ✅ 分类 / 标签 Redis 缓存（Cache-Aside + 主动失效）

---

## 步骤 6 · 缓存热点文章（P2）

### 6.1 需求（按点）

1. 判定**热点文章**：Redis ZSet 记录每篇文章的访问次数
2. **热点文章详情缓存**：Cache-Aside，命中缓存不查库
3. 前台展示**热门文章榜**

### 6.2 实现方案（按点）

1. 详情访问时 `ZINCRBY article:hot 1 <id>`（热点计数，Redis 很轻）
2. 详情缓存 `cache:article:detail:<id>`（TTL 30min），更新/删除时失效
3. `GET /api/articles/hot` 返回 Top N（`ZREVRANGE`；ZSet 无数据时回退为浏览量最高）
4. 前台首页新增「🔥 热门文章」板块（前 3 名橙色排名）

### 6.3 完成情况

- ✅ 后端 + 前端 BUILD SUCCESS
- ✅ 热点判定 + 详情缓存 + 热门榜接口 + 前台展示

---

## 步骤 7 · RAG 基础链路（2.1~2.3）：切片 → 向量化 → 入库 → 检索

### 7.1 需求（按点）

1. **本地部署**：Ollama（`qwen3:1.7b` 生成 + `mxbai-embed-large` 向量化）+ Qdrant（向量库）
2. 文章按标题切片，生成 chunk + 标题路径
3. 切片向量化后存入 Qdrant
4. 问题向量化后检索 Top N

### 7.2 实现方案（按点）

1. `RagProperties`：ollama/qdrant 地址、模型、集合名、向量维度（application.yml `rag.*`）
2. `EmbeddingService`：调 Ollama `/api/embed`（RestClient，无需 Spring AI）
3. `VectorStoreService`：Qdrant REST 建集合 / upsert / search
4. `ChunkService`：按 `##` 标题切片（简化版，后续可换 AST）
5. `RagService` + `RagController`：`POST /api/admin/rag/ingest` 全量入库；`GET /api/rag/search` 检索
6. `docker-compose.yml` 增加 qdrant 服务

### 7.3 完成情况

- ✅ Ollama + Qdrant 本地部署（全免费、离线）
- ✅ Python 端到端验证：4 段文本 → 向量（1024 维）→ 入库 → 问题检索命中（score 0.87）
- ✅ **Java 侧全链路打通**：`POST /api/admin/rag/ingest` 入库 18 个 chunk；`GET /api/rag/search` 检索命中（score 0.83）
- ✅ 后端 BUILD SUCCESS

### 7.4 排障记录（重要）

- **现象**：Java 的 PUT 请求"返回 2xx 但未真正写入 Qdrant"，GET/POST 正常
- **根因 1**：`target/classes` 残留旧 class（新旧代码混杂），`mvn spring-boot:run` 未 clean → 运行的并非最新代码。**必须用 `mvn clean spring-boot:run`**
- **根因 2**：JDK HttpClient 在本机对 PUT 行为异常 → 改用 `SimpleClientHttpRequestFactory`（HttpURLConnection）
- **经验**：排查时让请求"响亮失败"（打印原始响应 + 强制验证），不要静默吞错；改代码后务必 clean 重建

---

## 步骤 8 · RAG 问答（2.4）：检索 + LLM 生成

### 8.1 需求（按点）

1. 把检索到的内容交给大模型生成答案，形成 **RAG 闭环**
2. 返回「**答案 + 引用来源**」（哪篇文章的哪个章节）

### 8.2 实现方案（按点）

1. `ChatService`：调 Ollama `/api/chat`（`qwen3:1.7b`，system 定角色 + user 给资料和问题）
2. `RagService.ask`：检索 Top N → 组装 prompt（每条资料标注《文章》- 章节）→ 生成 → 返回 `AskResult{answer, references}`
3. `RagController`：新增 `GET /api/rag/ask?q=xxx&topK=3`
4. `RestClientConfig`：读超时加到 180s（本机 CPU 生成慢）

### 8.3 完成情况

- ✅ 后端 clean 构建 BUILD SUCCESS
- ✅ Python 端到端验证：问题 → 检索 3 chunk → qwen3:1.7b 生成高质量答案（准确涵盖 Java 17 / Jakarta EE / Initializr，无编造）
- ✅ `GET /api/rag/ask` 接口就绪

---

## 步骤 9 · SSE 流式问答前端（2.5）

### 9.1 需求（按点）

1. 后端 **SSE 流式接口**：逐 token 推送（打字机效果），不用干等 50 秒
2. 前端**问答页面**：输入问题 → 流式展示答案 → 显示引用来源

### 9.2 实现方案（按点）

1. `ChatService.chatStream`：Ollama `stream=true`，解析 NDJSON 流，逐 token 经 `SseEmitter` 推送（event 名 `token`）
2. `RagService.buildContext`：检索 + prompt 构建抽成公共方法，供流式/非流式共用
3. `GET /api/rag/ask/stream`：先推 token 事件 → 完成后推 `references` 事件 → complete
4. 前端 `api/rag.ts`：`EventSource` 订阅 `token` / `references` 事件
5. `RagPage`：问答界面（思考中光标 → 流式答案 → 引用来源链接可跳转文章）

### 9.3 完成情况

- ✅ 后端 + 前端构建通过
- ✅ 验证 qwen3 流式格式：`thinking`（内部推理）先行、`content`（正式答案）后出，content 最终非空
- ✅ `/api/rag/ask/stream` + 前端 `RagPage` 就绪

---

## 步骤 10 · 语料扩充 + 前端分页 + Query 改写

### 10.1 语料扩充
- 脚本批量生成并插入 **300 篇**文章（20 主题 × 模板组合），数据库共 306 篇（305 已发布）
- 全量切片向量化：**1526 个 chunk** 写入 Qdrant（批量 Embedding，16 分钟）
- 检索验证：问「Nginx 反向代理」命中 Nginx 新文章（score 0.93+）
- 说明：生成脚本未入库 git；语料为模板生成，用于验证大语料场景

### 10.2 前端分页
- 首页改为**分页展示**（每页 12 篇，上一页/下一页 + 共 N 篇），分类切换重置页码

### 10.3 Query 改写（多轮）
- `ChatService.rewriteQuery`：结合历史对话用 LLM 改写成独立查询
- `RagService.buildContext` / `ask` 支持 history 参数；`/api/rag/ask` 与 `/ask/stream` 均接受 `history`（JSON）
- 前端 `RagPage` 记录最近对话轮次并随请求传递（多轮改写 → 提升检索召回）

## 步骤 11 · 多轮会话（S1+S2：后端会话 + 前端最小接入）

### 11.1 完成情况

- **数据表**：新增 `conversation`（会话窗口）+ `message`（会话消息，含 references_json），启动自动建表
- **后端**：`ConversationService`（列表/新建/删除/消息/历史）+ `ConversationController`（`/api/rag/conversations`）
- **多轮问答**：`/api/rag/ask` 与 `/ask/stream` 支持 `conversationId` —— 从库加载历史 → Query 改写 → 检索 → 生成 → 保存 user/assistant 消息；新会话自动用首问更新标题
- **前端最小接入**：首次提问自动建会话；「新建会话」按钮重置；`askStream` 传 `conversationId`
- **待办（S3）**：完整会话侧边栏（列表/切换/删除）、历史消息气泡回显 —— 见 `task-conversation.md`

### 10.4 多路召回调研结论（本轮验证）
- Qdrant 1.18 支持全文检索与 RRF 融合，dense 向量 + RRF 融合已验证可用
- **BM25 text 查询的 API 格式与文档不一致**（`/points/query` 的 `{"query":{"text":...}}` 报错），待后续按 Qdrant 实际 API 调整；text 索引已创建
- 详见 `rag-advanced-design.md` §2
