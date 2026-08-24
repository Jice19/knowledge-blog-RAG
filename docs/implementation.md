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
