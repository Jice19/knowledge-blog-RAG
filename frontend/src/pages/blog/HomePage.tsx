import { useEffect, useState } from 'react'
import { pageArticles, type ArticleVO } from '../../api/article'
import { listCategories, type Category } from '../../api/category'
import ArticleCard from '../../components/ArticleCard'

export default function HomePage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [articles, setArticles] = useState<ArticleVO[]>([])
  const [activeId, setActiveId] = useState<number | 'all'>('all')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    listCategories()
      .then(setCategories)
      .catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    pageArticles({ page: 1, size: 100, categoryId: activeId === 'all' ? undefined : activeId })
      .then((data) => setArticles(data.records))
      .catch(() => setArticles([]))
      .finally(() => setLoading(false))
  }, [activeId])

  const pill = (id: number | 'all') =>
    `rounded-full px-4 py-1.5 text-sm font-medium transition ${
      activeId === id
        ? 'bg-indigo-600 text-white shadow-sm'
        : 'bg-white text-slate-600 border border-slate-200 hover:border-indigo-300 hover:text-indigo-600'
    }`

  return (
    <div>
      {/* Hero */}
      <section className="bg-gradient-to-br from-indigo-600 via-indigo-500 to-violet-600 text-white">
        <div className="mx-auto max-w-6xl px-4 py-16 text-center sm:px-6 sm:py-20">
          <h1 className="text-3xl font-bold tracking-tight sm:text-5xl">AI 智识博客</h1>
          <p className="mx-auto mt-4 max-w-2xl text-indigo-100 sm:text-lg">
            沉淀技术文章 · 用 RAG 让知识可被检索、可被问答
          </p>
        </div>
      </section>

      {/* 分类筛选 + 文章网格 */}
      <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
        <div className="flex flex-wrap items-center gap-2">
          <button onClick={() => setActiveId('all')} className={pill('all')}>
            全部
          </button>
          {categories.map((c) => (
            <button key={c.id} onClick={() => setActiveId(c.id)} className={pill(c.id)}>
              {c.name}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="py-24 text-center text-slate-400">加载中…</div>
        ) : (
          <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {articles.map((a) => (
              <ArticleCard key={a.id} article={a} />
            ))}
          </div>
        )}

        {!loading && articles.length === 0 && (
          <div className="py-24 text-center text-slate-400">该分类下暂无文章</div>
        )}
      </div>
    </div>
  )
}
