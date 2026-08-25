---
title: ThreadLocal 原理与内存泄漏
summary: 每个线程独立副本的实现方式，以及弱引用导致的内存泄漏与避免方法
category: Java
---

## 作用与原理

ThreadLocal 让每个线程拥有变量的独立副本，线程之间互不干扰。它常用于保存用户上下文、数据库连接等。实现上，每个 Thread 内部有一个 ThreadLocalMap，以 ThreadLocal 对象为 key 存值。

## 为什么可能内存泄漏

ThreadLocalMap 的 key 是弱引用，ThreadLocal 对象被回收后 key 变为 null，但 value 仍被线程强引用着。线程池中的线程长期存活，这些 value 无法被回收，就造成内存泄漏。

## 如何避免

规范做法是用完调用 remove() 方法清理。这正是鉴权拦截器在 afterCompletion 里执行 UserContext.clear() 的原因——请求结束清理 ThreadLocal，避免线程复用时读到上个请求的数据，也防止内存泄漏。
