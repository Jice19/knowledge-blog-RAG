import http from './http'

export interface LoginVO {
  token: string
  userId: number
  username: string
  nickname: string
  role: string
}

export interface LoginUser {
  userId: number
  username: string
  role: string
}

/** 登录，返回 JWT 与用户信息 */
export function login(data: { username: string; password: string }) {
  return http.post('/auth/login', data) as Promise<LoginVO>
}

/** 获取当前登录用户 */
export function me() {
  return http.get('/auth/me') as Promise<LoginUser>
}

/** 退出登录（后端把 token 拉黑） */
export function logout() {
  return http.post('/auth/logout')
}
