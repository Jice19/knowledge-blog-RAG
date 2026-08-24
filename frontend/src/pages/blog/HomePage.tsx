import { useEffect, useState } from 'react'
import { pageArticles, type ArticleVO } from '../../api/article'
import { listCategories, type Category } from '../../api/category'
import ArticleCard from '../../components/ArticleCard'

export default function HomePage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [articles, setArticles] = useState<ArticleVO[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const size = 12
  const [activeId, setActiveId] = useState<number | 'all'>('all')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    listCategories()
      .then(setCategories)
      .catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    pageArticles({ page, size, categoryId: activeId === 'all' ? undefined : activeId })
      .then((data) => {
        setArticles(data.records)
        setTotal(data.total)
      })
      .catch(() => setArticles([]))
      .finally(() => setLoading(false))
  }, [page, activeId])

  const totalPages = Math.max(1, Math.ceil(total / size))

  const selectCategory = (id: number | 'all') => {
    setActiveId(id)
    setPage(1)
  }

  const pill = (id: number | 'all') =>
    `rounded-full px-4 py-1.5 text-sm font-medium transition ${
      activeId === id
        ? 'bg-indigo-600 text-white shadow-sm'
        : 'bg-white text-slate-600 border border-slate-200 hover:border-indigo-300 hover:text-indigo-600'
    }`

  const pageBtn =
    'rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm transition hover:bg-slate-50 disabled:opacity-40'

  return (
    <div>
      <section className="bg-gradient-to-br from-indigo-600 via-indigo-500 to-violet-600 text-white">
        <div className="mx-auto max-w-6xl px-4 py-14 text-center sm:px-6">
          <h1 className="text-3xl font-bold tracking-tight sm:text-5xl">AI 智识博客</h1>
          <p className="mx-auto mt-4 max-w-2xl text-indigo-100 sm:text-lg">
            沉淀技术文章 · 用 RAG 让知识可被检索、可被问答
          </p>
        </div>
      </section>

      <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
        <div className="flex flex-wrap items-center gap-2">
          <button onClick={() => selectCategory('all')} className={pill('all')}>
            全部
          </button>
          {categories.map((c) => (
            <button key={c.id} onClick={() => selectCategory(c.id)} className={pill(c.id)}>
              {c.name}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="py-24 text-center text-slate-400">加载中…</div>
        ) : (
          <>
            <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {articles.map((a) => (
                <ArticleCard key={a.id} article={a} />
              ))}
            </div>

            {articles.length === 0 && (
              <div className="py-24 text-center text-slate-400">该分类下暂无文章</div>
            )}

            {/* 分页 */}
            {total > 0 && (
              <div className="mt-10 flex items-center justify-between border-t border-slate-200 pt-5 text-sm text-slate-500">
                <span>共 {total} 篇</span>
                <div className="flex items-center gap-3">
                  <button disabled={page <= 1} onClick={() => setPage(page - 1)} className={pageBtn}>
                    上一页
                  </button>
                  <span>
                    {page} / {totalPages}
                  </span>
                  <button disabled={page >= totalPages} onClick={() => setPage(page + 1)} className={pageBtn}>
                    下一页
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
