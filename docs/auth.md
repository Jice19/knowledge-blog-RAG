# 鉴权设计（JWT）

## 方案

**JWT（HS256）+ Redis 黑名单**，无状态鉴权，用 Redis 补齐「退出登录立即失效」。

## 认证流程

```
1. 登录   POST /api/auth/login
   → 查库 + BCrypt 校验 → 签发 JWT（含 userId / username / role，有效期 2h）

2. 请求   携带 Authorization: Bearer <token>

3. 拦截器 JwtAuthInterceptor
   → 黑名单校验（Redis hasKey）→ 解析 token → 写入 UserContext(ThreadLocal)

4. 退出   POST /api/auth/logout
   → token 写入 Redis 黑名单（TTL = token 剩余有效期）
```

## 接口

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | /api/auth/login | 否 | 登录，返回 JWT |
| GET | /api/auth/me | 是 | 获取当前用户 |
| POST | /api/auth/logout | 是 | 退出登录（拉黑 token） |

## 拦截路径

- **拦截**：`/api/admin/**`、`/api/auth/me`、`/api/auth/logout`
- **放行**：`/api/auth/login`（以及后续的公开接口，如 `/api/articles`）

## 默认账号

| 用户名 | 密码 | 说明 |
|---|---|---|
| admin | admin123 | 首次启动自动创建（见 `AdminInitializer`） |

## 关键实现

| 类 | 职责 |
|---|---|
| `JwtUtil` | HS256 签发/解析 token |
| `JwtAuthInterceptor` | 拦截器：黑名单校验 + 解析 + 写 ThreadLocal |
| `UserContext` | ThreadLocal 存当前登录用户 |
| `LoginUser` | record：userId / username / role |
| `PasswordConfig` | BCryptPasswordEncoder Bean |

## 安全说明

1. 密码 **BCrypt** 加密存储，绝不存明文。
2. token 有效期默认 2h（`jwt.expire-hours` 可配）。
3. **生产必须修改 `jwt.secret`**（当前是示例值）。
4. JWT 存 localStorage 有 XSS 风险，学习项目可接受；生产可改为 HttpOnly Cookie 存 JWT。
5. 退出登录靠 Redis 黑名单实现，需 Redis 在线。
