---
title: SpringBoot 自动配置原理
summary: 约定大于配置，@EnableAutoConfiguration 如何根据依赖自动装配 Bean
category: Spring
---

## 约定大于配置

SpringBoot 的核心思想是约定大于配置：引入某个 starter 依赖，框架就按默认约定帮你配好，省去大量 XML 配置。你只需在需要覆盖时改配置文件。

## 自动装配的机制

启动类上的 @SpringBootApplication 包含 @EnableAutoConfiguration。SpringBoot 启动时会加载所有 jar 里 META-INF/spring 目录下的配置类，根据条件注解判断是否生效。例如类路径里有 DataSource 类且没有自定义配置时，才自动配置数据源。

## 条件注解

@ConditionalOnClass、@ConditionalOnMissingBean 等注解是自动配置的开关。只有当某个类存在、某个 Bean 不存在时才装配。理解这一点，就能明白为什么引入 redis starter 后直接写 spring.redis.host 就能用了。
