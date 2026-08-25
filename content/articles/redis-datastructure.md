---
title: Redis 常见数据结构与应用场景
summary: 五种基础数据结构的特点，以及它们对应的典型业务场景
category: Redis
---

## 五种基础结构

Redis 提供字符串 String、哈希 Hash、列表 List、集合 Set、有序集合 ZSet。String 存单个值；Hash 存对象字段；List 是有序队列；Set 是无序去重集合；ZSet 带分数可排序。

## 典型场景

String 适合缓存和计数器；Hash 适合存对象（如用户信息）；List 适合消息队列、最新列表；Set 适合去重、共同好友；ZSet 适合排行榜、热点内容排序，因为按分数范围查询非常高效。

## 选型要点

选择数据结构要看操作的复杂度。例如要按浏览量排序就选 ZSet，只需 O(1) 的 incrementScore 计数，再用 reverseRange 取 Top N。而纯去重用 Set 更轻量。数据结构选对，性能和代码都会简单很多。
