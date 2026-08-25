---
title: 全局异常处理与统一响应体设计
summary: @RestControllerAdvice 统一捕获异常，Result 统一返回结构
category: Spring
---

## 为什么需要统一处理

如果每个接口都自己 try catch，代码会充满重复的错误处理，返回结构也五花八门，前端难以对接。统一异常处理能把错误兜底收敛到一处。

## 全局异常拦截

用 @RestControllerAdvice 加 @ExceptionHandler 拦截异常：业务异常抛 BusinessException 返回对应错误码；参数校验异常 MethodArgumentNotValidException 返回校验信息；最后的 Exception 兜底所有未预期异常，返回统一的服务端错误。

## 统一响应体

所有接口返回 Result 结构：code、message、data 三个字段。成功时 code 为 200 且 data 携带数据；失败时 data 为空且 message 描述原因。前端只需判断 code 即可统一处理，出错时提示 message，对接到本项目的 Axios 拦截器非常方便。
