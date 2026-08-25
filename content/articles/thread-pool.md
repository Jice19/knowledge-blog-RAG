---
title: Java 线程池核心参数详解
summary: 七个参数的含义、任务执行流程，以及为什么建议手动创建线程池
category: Java
---

## 七个核心参数

ThreadPoolExecutor 的核心参数是：核心线程数 corePoolSize、最大线程数 maximumPoolSize、空闲线程存活时间 keepAliveTime 与单位、任务队列 workQueue、线程工厂 threadFactory、拒绝策略 handler。

## 任务执行流程

提交任务时，先看核心线程是否已满；未满则新建核心线程执行，满了则入队。队列也满了才创建非核心线程，直到达到最大线程数。还是满了就触发拒绝策略，常见的拒绝策略有抛异常、丢弃任务、让调用线程自己执行等。

## 为什么不要用 Executors 快捷方法

Executors.newFixedThreadPool 使用无界队列，任务堆积会 OOM；newCachedThreadPool 最大线程数是 Integer.MAX_VALUE，可能创建海量线程。所以规范要求通过 ThreadPoolExecutor 显式指定参数，让线程数、队列长度、拒绝策略都可控。
