---
title: JWT 无状态鉴权原理与实现
summary: JWT 的结构、无状态特性，以及拦截器 + ThreadLocal 实现鉴权的完整流程
category: Java
---

## 什么是 JWT

JWT（JSON Web Token）是一种无状态鉴权方案。服务端登录成功后不保存会话，而是把用户信息用密钥签名后编码进 token 返回给客户端。之后每次请求客户端都在 Authorization 头携带 token，服务端验证签名与过期时间即可识别用户。

## JWT 的结构

JWT 由三段组成，用点号分隔：Header、Payload、Signature。Header 声明签名算法（如 HS256）；Payload 存放 userId、角色、过期时间等声明；Signature 用服务端密钥对前两段做 HMAC 签名。签名的作用是防篡改——任何一段被改，签名就对不上。

## 无状态的好处与代价

无状态意味着服务端不用存 session，水平扩容简单，多台机器共享同一密钥即可校验。代价是 token 一旦签发就无法主动作废，除非引入黑名单（把登出的 token 存进 Redis 并设过期时间）。

## 拦截器实现要点

用 HandlerInterceptor 统一校验：从 Authorization 头取出 token，先查 Redis 黑名单，再解析签名和过期时间，最后把 userId、role 写入 ThreadLocal，供后续业务使用。请求结束后必须在 afterCompletion 里清理 ThreadLocal，避免线程复用导致用户串号。
