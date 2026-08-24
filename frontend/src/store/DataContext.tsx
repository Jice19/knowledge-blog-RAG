import { createContext, useContext, useState, type ReactNode } from 'react'
import type { Article, Category, Tag } from '../types'
import { mockArticles, mockCategories, mockTags } from '../data/mock'

interface DataContextValue {
  articles: Article[]
  categories: Category[]
  tags: Tag[]
  addArticle: (a: Article) => void
  updateArticle: (a: Article) => void
  deleteArticle: (id: number) => void
  addCategory: (c: Category) => void
  deleteCategory: (id: number) => void
  addTag: (t: Tag) => void
  deleteTag: (id: number) => void
}

const DataContext = createContext<DataContextValue | null>(null)

export function DataProvider({ children }: { children: ReactNode }) {
  const [articles, setArticles] = useState<Article[]>(mockArticles)
  const [categories, setCategories] = useState<Category[]>(mockCategories)
  const [tags, setTags] = useState<Tag[]>(mockTags)

  const addArticle = (a: Article) => setArticles((prev) => [a, ...prev])
  const updateArticle = (a: Article) =>
    setArticles((prev) => prev.map((x) => (x.id === a.id ? a : x)))
  const deleteArticle = (id: number) =>
    setArticles((prev) => prev.filter((x) => x.id !== id))

  const addCategory = (c: Category) => setCategories((prev) => [...prev, c])
  const deleteCategory = (id: number) =>
    setCategories((prev) => prev.filter((x) => x.id !== id))

  const addTag = (t: Tag) => setTags((prev) => [...prev, t])
  const deleteTag = (id: number) => setTags((prev) => prev.filter((x) => x.id !== id))

  return (
    <DataContext.Provider
      value={{
        articles,
        categories,
        tags,
        addArticle,
        updateArticle,
        deleteArticle,
        addCategory,
        deleteCategory,
        addTag,
        deleteTag,
      }}
    >
      {children}
    </DataContext.Provider>
  )
}

export function useData() {
  const ctx = useContext(DataContext)
  if (!ctx) throw new Error('useData 必须在 DataProvider 内使用')
  return ctx
}
