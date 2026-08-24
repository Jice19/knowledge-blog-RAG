package com.jice19.blog.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jice19.blog.entity.Article;
import com.jice19.blog.entity.ArticleTag;
import com.jice19.blog.entity.Category;
import com.jice19.blog.entity.Tag;
import com.jice19.blog.entity.User;
import com.jice19.blog.mapper.ArticleMapper;
import com.jice19.blog.mapper.ArticleTagMapper;
import com.jice19.blog.mapper.CategoryMapper;
import com.jice19.blog.mapper.TagMapper;
import com.jice19.blog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首次启动：初始化种子数据（分类/标签/示例文章），便于预览。
 * 仅在 category 表为空时执行，幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class DataInitializer implements CommandLineRunner {

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void run(String... args) {
        Long catCount = categoryMapper.selectCount(null);
        if (catCount != null && catCount > 0) {
            return;
        }
        seedCategories();
        seedTags();
        seedArticles();
        log.info("已初始化种子数据：6 个分类 / 10 个标签 / 6 篇文章");
    }

    private void seedCategories() {
        String[][] data = {
                {"后端开发", "backend", "1"},
                {"前端开发", "frontend", "2"},
                {"数据库", "database", "3"},
                {"中间件", "middleware", "4"},
                {"AI / RAG", "ai", "5"},
                {"架构设计", "architecture", "6"},
        };
        for (String[] d : data) {
            Category c = new Category();
            c.setName(d[0]);
            c.setSlug(d[1]);
            c.setSort(Integer.parseInt(d[2]));
            categoryMapper.insert(c);
        }
    }

    private void seedTags() {
        String[] names = {"SpringBoot", "Redis", "React", "TypeScript", "MySQL",
                "RabbitMQ", "Milvus", "RAG", "性能优化", "鉴权"};
        for (String n : names) {
            Tag t = new Tag();
            t.setName(n);
            tagMapper.insert(t);
        }
    }

    private void seedArticles() {
        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        Long authorId = admin != null ? admin.getId() : 1L;

        Map<String, Long> catMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getSlug, Category::getId));
        Map<String, Long> tagMap = tagMapper.selectList(null).stream()
                .collect(Collectors.toMap(Tag::getName, Tag::getId));

        addArticle("Spring Boot 3 快速上手指南",
                "从环境准备到第一个 RESTful 接口，快速跑通 Spring Boot 3，并避开 Java 17 与 Jakarta 迁移的坑。",
                """
                Spring Boot 3 是 Java 后端开发的主流框架，要求 **Java 17** 起步。本文带你快速搭建一个 RESTful 服务。

                ## 环境准备

                - JDK 17+
                - Maven 3.8+
                - IDEA

                ## 创建项目

                使用 Spring Initializr，选择依赖 `spring-boot-starter-web`：

                ```xml
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>3.4.0</version>
                </parent>
                ```

                ## 第一个接口

                ```java
                @RestController
                public class HelloController {
                    @GetMapping("/hello")
                    public String hello() {
                        return "Hello, Spring Boot 3!";
                    }
                }
                ```

                > 注意：Spring Boot 3 已全面迁移到 Jakarta EE，`javax.*` 需替换为 `jakarta.*`。
                """,
                catMap.get("backend"), List.of("SpringBoot", "鉴权"), 1, authorId, tagMap);

        addArticle("Redis 缓存穿透、击穿、雪崩全解析",
                "缓存三大经典问题的成因与解决思路：空值缓存、布隆过滤器、互斥锁、过期时间抖动，一文讲透。",
                """
                Redis 作为缓存层能显著降低数据库压力，但会引入三类经典问题。

                ## 缓存穿透

                查询一个**根本不存在**的数据，每次都打到数据库。

                **解决**：缓存空值 + 布隆过滤器。

                ## 缓存击穿

                某个热点 key 过期瞬间，大量请求同时打到数据库。

                **解决**：互斥锁、逻辑过期。

                ## 缓存雪崩

                大量 key 同一时间过期，或 Redis 宕机。

                **解决**：过期时间加随机值、多级缓存、限流降级。

                ## 小结

                - 穿透：防不存在数据
                - 击穿：防热点失效
                - 雪崩：防批量失效
                """,
                catMap.get("database"), List.of("Redis", "性能优化"), 1, authorId, tagMap);

        addArticle("React 18 并发特性与实战",
                "createRoot、自动批处理、useTransition、useDeferredValue —— React 18 并发渲染带来的真实收益。",
                """
                React 18 带来了**并发渲染**能力，让 UI 更新可中断、可优先级调度。

                ## 新特性一览

                - `createRoot` 替代 `ReactDOM.render`
                - 自动批处理
                - `useTransition` / `useDeferredValue`
                - `Suspense` 增强

                ## useTransition 标记非紧急更新

                ```tsx
                const [isPending, startTransition] = useTransition()
                startTransition(() => setQuery(input))
                ```

                > 核心思想：把「紧急交互」与「昂贵渲染」解耦，让输入框保持流畅。
                """,
                catMap.get("frontend"), List.of("React", "TypeScript"), 1, authorId, tagMap);

        addArticle("向量数据库选型：Milvus vs Qdrant vs pgvector",
                "RAG 落地第一步是选向量库。从资源占用、部署难度、SQL 友好度三个维度对比，给出选型建议。",
                """
                RAG 的核心是向量检索，而向量库选型直接影响成本与性能。

                | 方案 | 定位 | 特点 |
                | --- | --- | --- |
                | Milvus | 分布式云原生 | 功能强、资源重 |
                | Qdrant | 单二进制 | 轻量、易部署 |
                | pgvector | PostgreSQL 扩展 | SQL 友好 |

                ## 选型建议

                - 学习 / 小规模：**Qdrant** 或 **pgvector**
                - 生产大规模：**Milvus**
                - 已有 PG：pgvector 零迁移成本

                > 300 篇文章规模的博客，暴力检索都够用，选型重点在「学习成本」而非「性能」。
                """,
                catMap.get("ai"), List.of("Milvus", "RAG"), 1, authorId, tagMap);

        addArticle("RabbitMQ 消息可靠性如何保证",
                "从发送端 confirm、消费端手动 ack 到死信队列，构建一条消息不丢、不重复消费的可靠链路。",
                """
                消息队列解耦了生产者与消费者，但**消息不丢**是基本要求。

                ## 发送端可靠性

                - 开启 confirm 模式
                - 失败重试 + 落库兜底

                ## 消费端可靠性

                - 手动 ack，业务成功后再确认
                - 失败重试 + 死信队列

                ## 幂等

                重复消费是常态，业务侧需配合**幂等表**或唯一约束去重。
                """,
                catMap.get("middleware"), List.of("RabbitMQ", "性能优化"), 1, authorId, tagMap);

        addArticle("JWT 鉴权设计与安全实践",
                "无状态鉴权的结构、BCrypt 密码存储、Token 有效期与黑名单，前后端分离架构下的安全细节。",
                """
                JWT 是无状态的鉴权方案，适合前后端分离架构。

                ## 结构

                `Header.Payload.Signature` 三段 Base64 编码。

                ## 实践要点

                - 密码用 BCrypt 加密存储
                - Token 有效期不宜过长
                - 退出登录需配合黑名单（Redis）

                > 本文为草稿示例，用于演示「草稿」状态。
                """,
                catMap.get("backend"), List.of("SpringBoot", "鉴权"), 0, authorId, tagMap);
    }

    private void addArticle(String title, String summary, String content, Long categoryId,
                            List<String> tagNames, int status, Long authorId, Map<String, Long> tagMap) {
        Article a = new Article();
        a.setTitle(title);
        a.setSummary(summary);
        a.setContent(content);
        a.setCategoryId(categoryId);
        a.setAuthorId(authorId);
        a.setStatus(status);
        a.setViewCount(0);
        articleMapper.insert(a);
        for (String tn : tagNames) {
            Long tagId = tagMap.get(tn);
            if (tagId != null) {
                ArticleTag at = new ArticleTag();
                at.setArticleId(a.getId());
                at.setTagId(tagId);
                articleTagMapper.insert(at);
            }
        }
    }
}
