import http from './http'

export interface Category {
  id: number
  name: string
  slug: string
  sort: number
  createTime?: string
  updateTime?: string
}

export interface CategoryDTO {
  name: string
  slug: string
  sort?: number
}

/** 分类列表（公开） */
export function listCategories() {
  return http.get('/categories') as Promise<Category[]>
}

export function createCategory(data: CategoryDTO) {
  return http.post('/admin/categories', data)
}

export function updateCategory(id: number, data: CategoryDTO) {
  return http.put(`/admin/categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return http.delete(`/admin/categories/${id}`)
}
