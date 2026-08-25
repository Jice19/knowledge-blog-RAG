---
title: SSE 服务端推送原理
summary: 单向实时推送如何用 Server-Sent Events 实现流式输出
category: RAG
---

## SSE 是什么

SSE（Server-Sent Events）是基于 HTTP 的服务端向客户端单向推送技术。客户端用 EventSource 建立连接，服务端保持长连接并持续发送事件，适合实时通知、流式输出等场景。

## 与 WebSocket 的区别

WebSocket 是全双工双向通信，适合聊天等需要客户端频繁主动发消息的场景。SSE 是单向的，客户端只能接收，但实现更简单，自动重连，走普通 HTTP 协议。问答流式输出只需要服务端推送，SSE 更轻量合适。

## 流式问答的实现

大模型生成答案是一段一段输出的。服务端用 SseEmitter 把每个 token 作为事件推给前端，前端实时渲染成打字机效果，生成结束后再推一个 references 事件返回引用来源。本项目正是用这套机制实现"打字机式"答案展示。
