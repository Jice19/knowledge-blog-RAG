import { useMemo, useState } from 'react'
import { useData } from '../../store/DataContext'
import ArticleCard from '../../components/ArticleCard'

export default function HomePage() {
  const { articles, categories } = useData()
  const [activeSlug, setActiveSlug] = useState<string>('all')

  const published = useMemo(() => articles.filter((a) => a.status === 1), [articles])
  const filtered =
    activeSlug === 'all' ? published : published.filter((a) => a.category?.slug === activeSlug)

  const pill = (slug: string) =>
    `rounded-full px-4 py-1.5 text-sm font-medium transition ${
      activeSlug === slug
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
          <div className="mx-auto mt-8 flex max-w-xl items-center gap-2 rounded-full border border-white/20 bg-white/10 px-5 py-3 backdrop-blur">
            <svg className="h-5 w-5 text-indigo-200" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" strokeLinecap="round" />
            </svg>
            <input
              placeholder="输入问题，检索全站知识…"
              className="flex-1 bg-transparent text-white outline-none placeholder:text-indigo-200"
            />
            <button className="rounded-full bg-white px-4 py-1.5 text-sm font-medium text-indigo-600 transition hover:bg-indigo-50">
              提问
            </button>
          </div>
        </div>
      </section>

      {/* 分类筛选 + 文章网格 */}
      <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
        <div className="flex flex-wrap items-center gap-2">
          <button onClick={() => setActiveSlug('all')} className={pill('all')}>
            全部
          </button>
          {categories.map((c) => (
            <button key={c.id} onClick={() => setActiveSlug(c.slug)} className={pill(c.slug)}>
              {c.name}
            </button>
          ))}
        </div>

        <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((a) => (
            <ArticleCard key={a.id} article={a} />
          ))}
        </div>

        {filtered.length === 0 && (
          <div className="py-24 text-center text-slate-400">该分类下暂无文章</div>
        )}
      </div>
    </div>
  )
}
