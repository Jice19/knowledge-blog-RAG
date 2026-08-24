# 数据库设计

## 基本信息

- 库名：`ai_knowledge_blog`
- 引擎：InnoDB，字符集 `utf8mb4`
- 建表脚本：`backend/src/main/resources/db/schema.sql`（幂等，启动时自动执行）

## 表结构

### 1. user（用户表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名，唯一 |
| password | VARCHAR(100) | BCrypt 加密后的密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像 URL |
| role | VARCHAR(20) | 角色，默认 `ADMIN` |
| create_time | DATETIME | 创建时间（DB 默认） |
| update_time | DATETIME | 更新时间（DB 默认） |

### 2. category（分类表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键，自增 |
| name | VARCHAR(50) | 分类名，唯一 |
| slug | VARCHAR(50) | URL 标识，唯一 |
| sort | INT | 排序 |
| create_time / update_time | DATETIME | 时间戳 |

### 3. tag（标签表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键，自增 |
| name | VARCHAR(50) | 标签名，唯一 |
| create_time | DATETIME | 创建时间 |

### 4. article（文章表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键，自增 |
| title | VARCHAR(200) | 标题 |
| summary | VARCHAR(500) | 摘要 |
| content | LONGTEXT | **Markdown 原文**（Phase 2 切片的输入源） |
| cover | VARCHAR(255) | 封面 URL |
| category_id | BIGINT | 分类 ID |
| author_id | BIGINT | 作者 ID |
| status | TINYINT | 0 草稿 / 1 已发布 |
| view_count | INT | 浏览量 |
| create_time / update_time | DATETIME | 时间戳 |

索引：`idx_category`、`idx_status`、`idx_author`

### 5. article_tag（文章-标签关联表）

| 字段 | 类型 | 说明 |
|---|---|---|
| article_id | BIGINT | 文章 ID（联合主键） |
| tag_id | BIGINT | 标签 ID（联合主键） |

## 设计说明

1. **article.content 存 Markdown 原文**，是 Phase 2「切片 → Embedding → 向量化」的输入，务必保持 Markdown 语义，不要转 HTML。
2. **article.status** 区分草稿/发布，Phase 2 只有「已发布」才进向量库。
3. **时间字段**由 DB 默认值填充（`DEFAULT CURRENT_TIMESTAMP` + `ON UPDATE`），实体层依赖 MyBatis-Plus 默认的「null 字段不参与 INSERT」策略，无需 MetaObjectHandler。
4. `user` 是 MySQL 关键字，实体用 `@TableName("`user`")` 反引号转义。
