import http from './http'

export interface Tag {
  id: number
  name: string
  createTime?: string
}

export interface TagDTO {
  name: string
}

/** 标签列表（公开） */
export function listTags() {
  return http.get('/tags') as Promise<Tag[]>
}

export function createTag(data: TagDTO) {
  return http.post('/admin/tags', data)
}

export function updateTag(id: number, data: TagDTO) {
  return http.put(`/admin/tags/${id}`, data)
}

export function deleteTag(id: number) {
  return http.delete(`/admin/tags/${id}`)
}
