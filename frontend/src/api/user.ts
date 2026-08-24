import http from './http'

export interface UserVO {
  id: number
  username: string
  nickname: string
  avatar: string
  role: string
  createTime: string
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

export interface UserDTO {
  username: string
  password?: string
  nickname?: string
  role?: string
}

/** 分页查询用户 */
export function pageUsers(params: { page: number; size: number; keyword?: string }) {
  return http.get('/admin/users', { params }) as Promise<PageResult<UserVO>>
}

/** 新增用户 */
export function createUser(data: UserDTO) {
  return http.post('/admin/users', data)
}

/** 修改用户 */
export function updateUser(id: number, data: UserDTO) {
  return http.put(`/admin/users/${id}`, data)
}

/** 删除用户 */
export function deleteUser(id: number) {
  return http.delete(`/admin/users/${id}`)
}
