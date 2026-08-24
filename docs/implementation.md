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
