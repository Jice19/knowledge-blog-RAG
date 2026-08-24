import type { Article, Category, Tag, User } from '../types'

export const mockUser: User = {
  id: 1,
  username: 'jice19',
  nickname: 'Jice',
  avatar: '',
}

export const mockCategories: Category[] = [
  { id: 1, name: '后端开发', slug: 'backend', sort: 1 },
  { id: 2, name: '前端开发', slug: 'frontend', sort: 2 },
  { id: 3, name: '数据库', slug: 'database', sort: 3 },
  { id: 4, name: '中间件', slug: 'middleware', sort: 4 },
  { id: 5, name: 'AI / RAG', slug: 'ai', sort: 5 },
  { id: 6, name: '架构设计', slug: 'architecture', sort: 6 },
]

export const mockTags: Tag[] = [
  { id: 1, name: 'SpringBoot' },
  { id: 2, name: 'Redis' },
  { id: 3, name: 'React' },
  { id: 4, name: 'TypeScript' },
  { id: 5, name: 'MySQL' },
  { id: 6, name: 'RabbitMQ' },
  { id: 7, name: 'Milvus' },
  { id: 8, name: 'RAG' },
  { id: 9, name: '性能优化' },
  { id: 10, name: '鉴权' },
]

const c = (id: number) => mockCategories.find((x) => x.id === id)

export const mockArticles: Article[] = [
  {
    id: 1,
    title: 'Spring Boot 3 快速上手指南',
    summary: '从环境准备到第一个 RESTful 接口，快速跑通 Spring Boot 3，并避开 Java 17 与 Jakarta 迁移的常见坑。',
    content: `Spring Boot 3 是 Java 后端开发的主流框架，要求 **Java 17** 起步。本文带你快速搭建一个 RESTful 服务。

## 环境准备

- JDK 17+
- Maven 3.8+
- IDEA

## 创建项目

使用 Spring Initializr，选择依赖 \`spring-boot-starter-web\`：

\`\`\`xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.4.0</version>
</parent>
\`\`\`

## 第一个接口

\`\`\`java
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot 3!";
    }
}
\`\`\`

> 注意：Spring Boot 3 已全面迁移到 Jakarta EE，\`javax.*\` 需替换为 \`jakarta.*\`。

启动后访问 \`http://localhost:8080/hello\` 即可看到返回。`,
    cover: '',
    categoryId: 1,
    tags: [mockTags[0], mockTags[9]],
    author: mockUser,
    status: 1,
    viewCount: 1284,
    createTime: '2026-07-18',
  },
  {
    id: 2,
    title: 'Redis 缓存穿透、击穿、雪崩全解析',
    summary: '缓存三大经典问题的成因与解决思路：空值缓存、布隆过滤器、互斥锁、过期时间抖动，一文讲透。',
    content: `Redis 作为缓存层能显著降低数据库压力，但会引入三类经典问题。

## 缓存穿透

查询一个**根本不存在**的数据，每次都打到数据库。

**解决**：缓存空值 + 布隆过滤器。

## 缓存击穿

某个热点 key 过期瞬间，大量请求同时打到数据库。

**解决**：互斥锁、逻辑过期。

## 缓存雪崩

大量 key 同一时间过期，或 Redis 宕机。

**解决**：过期时间加随机值、多级缓存、限流降级。

\`\`\`java
// 过期时间加随机抖动，避免同时失效
long ttl = 3600 + ThreadLocalRandom.current().nextInt(300);
\`\`\`

## 小结

- 穿透：防不存在数据
- 击穿：防热点失效
- 雪崩：防批量失效`,
    cover: '',
    categoryId: 3,
    tags: [mockTags[1], mockTags[8]],
    author: mockUser,
    status: 1,
    viewCount: 986,
    createTime: '2026-07-15',
  },
  {
    id: 3,
    title: 'React 18 并发特性与实战',
    summary: 'createRoot、自动批处理、useTransition、useDeferredValue —— React 18 并发渲染带来的真实收益。',
    content: `React 18 带来了**并发渲染**能力，让 UI 更新可中断、可优先级调度。

## 新特性一览

- \`createRoot\` 替代 \`ReactDOM.render\`
- 自动批处理
- \`useTransition\` / \`useDeferredValue\`
- \`Suspense\` 增强

## 自动批处理

\`\`\`tsx
// React 18 中两次 setState 只会触发一次渲染
setName('Jice')
setAge(22)
\`\`\`

## useTransition 标记非紧急更新

\`\`\`tsx
const [isPending, startTransition] = useTransition()
startTransition(() => setQuery(input))
\`\`\`

> 核心思想：把"紧急交互"与"昂贵渲染"解耦，让输入框保持流畅。`,
    cover: '',
    categoryId: 2,
    tags: [mockTags[2], mockTags[3]],
    author: mockUser,
    status: 1,
    viewCount: 1532,
    createTime: '2026-07-12',
  },
  {
    id: 4,
    title: '向量数据库选型：Milvus vs Qdrant vs pgvector',
    summary: 'RAG 落地第一步是选向量库。从资源占用、部署难度、SQL 友好度三个维度做对比，并给出选型建议。',
    content: `RAG 的核心是向量检索，而向量库选型直接影响成本与性能。

| 方案 | 定位 | 特点 |
| --- | --- | --- |
| Milvus | 分布式云原生 | 功能强、资源重 |
| Qdrant | 单二进制 | 轻量、易部署 |
| pgvector | PostgreSQL 扩展 | SQL 友好 |

## 选型建议

- 学习 / 小规模：**Qdrant** 或 **pgvector**
- 生产大规模：**Milvus**
- 已有 PG：pgvector 零迁移成本

> 300 篇文章规模的博客，暴力检索都够用，选型重点在"学习成本"而非"性能"。`,
    cover: '',
    categoryId: 5,
    tags: [mockTags[6], mockTags[7]],
    author: mockUser,
    status: 1,
    viewCount: 2105,
    createTime: '2026-07-08',
  },
  {
    id: 5,
    title: 'RabbitMQ 消息可靠性如何保证',
    summary: '从发送端 confirm、消费端手动 ack 到死信队列，构建一条消息不丢、不重复消费的可靠链路。',
    content: `消息队列解耦了生产者与消费者，但**消息不丢**是基本要求。

## 发送端可靠性

- 开启 confirm 模式
- 失败重试 + 落库兜底

## 消费端可靠性

- 手动 ack，业务成功后再确认
- 失败重试 + 死信队列

\`\`\`yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    listener:
      simple:
        acknowledge-mode: manual
\`\`\`

## 幂等

重复消费是常态，业务侧需配合**幂等表**或唯一约束去重。`,
    cover: '',
    categoryId: 4,
    tags: [mockTags[5], mockTags[8]],
    author: mockUser,
    status: 1,
    viewCount: 742,
    createTime: '2026-07-05',
  },
  {
    id: 6,
    title: 'JWT 鉴权设计与安全实践',
    summary: '无状态鉴权的结构、BCrypt 密码存储、Token 有效期与二次校验，前后端分离架构下的安全细节。',
    content: `JWT 是无状态的鉴权方案，适合前后端分离架构。

## 结构

\`Header.Payload.Signature\` 三段 Base64 编码。

## 实践要点

- 密码用 BCrypt 加密存储
- Token 有效期不宜过长
- 敏感操作二次校验
- 退出登录需配合黑名单（Redis）

\`\`\`java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode(rawPassword);
\`\`\`

> 本文为草稿，待补充代码示例后发布。`,
    cover: '',
    categoryId: 1,
    tags: [mockTags[0], mockTags[9]],
    author: mockUser,
    status: 0,
    viewCount: 0,
    createTime: '2026-07-20',
  },
]

// 为文章挂上分类对象（便于卡片/详情直接读取分类名与 slug）
mockArticles.forEach((a) => {
  a.category = c(a.categoryId)
})
