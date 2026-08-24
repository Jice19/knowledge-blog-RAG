export interface Category {
  id: number
  name: string
  slug: string
  sort: number
}

export interface Tag {
  id: number
  name: string
}

export interface User {
  id: number
  username: string
  nickname: string
  avatar: string
}

/** 0 草稿 / 1 已发布 */
export type ArticleStatus = 0 | 1

export interface Article {
  id: number
  title: string
  summary: string
  content: string
  cover: string
  categoryId: number
  category?: Category
  tags: Tag[]
  author: User
  status: ArticleStatus
  viewCount: number
  createTime: string
}
