---
title: MyBatis-Plus 条件构造器与分页
summary: 免写 SQL 的 CRUD 与 LambdaQueryWrapper，以及物理分页插件
category: Spring
---

## 为什么用 MyBatis-Plus

MyBatis-Plus 在 MyBatis 之上提供了通用 Mapper 和条件构造器，单表 CRUD 不用写 SQL。继承 BaseMapper 后，selectById、insert、updateById 等方法开箱即用，大幅减少样板代码。

## 条件构造器

LambdaQueryWrapper 用 lambda 表达式引用字段，避免手写字段名出错。例如 .eq(Article::getStatus, 1).orderByDesc(Article::getViewCount) 就能拼出条件查询。它比写字符串 SQL 更安全、可读性更高。

## 物理分页

通过分页插件，selectPage 会自动在 SQL 后追加 LIMIT 子句，并同时查出总记录数。列表接口返回 PageResult，包含 total、当前页、每页大小和记录列表，前端据此渲染分页条。物理分页避免了一次性查全表再在内存里切片。
