---
title: RESTful API 设计规范
summary: 用 HTTP 方法表达语义，资源化 URL 设计，统一状态码
category: 网络
---

## 资源与动词分离

RESTful 的核心是把 URL 当作资源，用 HTTP 方法表达动作：GET 查询、POST 新增、PUT 修改、DELETE 删除。例如文章资源统一是 /api/articles，增删改查都指向它，用不同方法区分，而不是在 URL 里写动词。

## URL 设计

资源名用名词复数，层级关系用路径表达，如 /api/articles/{id} 表示某篇文章。过滤、分页用查询参数，如 ?page=1&size=10&categoryId=3。避免在 URL 里堆砌操作动词，让接口语义清晰一致。

## 统一返回与状态码

用 HTTP 状态码表达结果，如 200 成功、400 参数错误、401 未认证、404 不存在、500 服务端错误。业务层再包一层统一结构 {code, message, data}，前端据此统一处理。本项目的 Result 包装和 JWT 鉴权就是这套规范的落地。
