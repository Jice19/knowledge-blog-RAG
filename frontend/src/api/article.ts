import http from './http'

export interface TagVO {
  id: number
  name: string
}

export interface ArticleVO {
  id: number
  title: string
  summary: string
  content: string
  cover: string
  categoryId: number | null
  categoryName: string | null
  categorySlug: string | null
  tags: TagVO[]
  authorId: number
  authorName: string
  status: number
  viewCount: number
  vectorStatus?: number
  createTime: string
  updateTime: string
}

export interface ArticleDTO {
  title: string
  summary?: string
  content: string
  cover?: string
  categoryId?: number | null
  tagIds?: number[]
  status: number
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

/** 前台：分页查已发布文章 */
export function pageArticles(params: { page: number; size: number; categoryId?: number }) {
  return http.get('/articles', { params }) as Promise<PageResult<ArticleVO>>
}

/** 前台：文章详情 */
export function getArticle(id: number) {
  return http.get(`/articles/${id}`) as Promise<ArticleVO>
}

/** 前台：热门文章 Top N */
export function hotArticles(limit = 5) {
  return http.get('/articles/hot', { params: { limit } }) as Promise<ArticleVO[]>
}

/** 后台：分页查全部文章 */
export function adminPageArticles(params: { page: number; size: number; status?: number }) {
  return http.get('/admin/articles', { params }) as Promise<PageResult<ArticleVO>>
}

/** 后台：按 id 查文章 */
export function getAdminArticle(id: number) {
  return http.get(`/admin/articles/${id}`) as Promise<ArticleVO>
}

export function createArticle(data: ArticleDTO) {
  return http.post('/admin/articles', data)
}

export function updateArticle(id: number, data: ArticleDTO) {
  return http.put(`/admin/articles/${id}`, data)
}

export function deleteArticle(id: number) {
  return http.delete(`/admin/articles/${id}`)
}

export function deleteBatchArticles(ids: number[]) {
  return http.delete('/admin/articles/batch', { data: ids })
}
