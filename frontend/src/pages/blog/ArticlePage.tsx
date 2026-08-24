import { Link, useParams } from 'react-router-dom'
import { useData } from '../../store/DataContext'
import Markdown from '../../components/Markdown'
import StatusBadge from '../../components/StatusBadge'
import { coverGradient } from '../../utils/cover'

export default function ArticlePage() {
  const { id } = useParams()
  const { articles } = useData()
  const article = articles.find((a) => a.id === Number(id))

  if (!article) {
    return <div className="mx-auto max-w-3xl px-4 py-24 text-center text-slate-400">文章不存在</div>
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6">
      <Link to="/" className="inline-flex items-center gap-1 text-sm text-slate-500 transition hover:text-indigo-600">
        ← 返回首页
      </Link>

      {/* 头部 */}
      <header className="mt-6">
        <div className="flex items-center gap-2">
          {article.category && (
            <Link
              to={`/category/${article.category.slug}`}
              className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-medium text-indigo-600 transition hover:bg-indigo-100"
            >
              {article.category.name}
            </Link>
          )}
          <StatusBadge status={article.status} />
        </div>

        <h1 className="mt-4 text-3xl font-bold leading-tight tracking-tight text-slate-900 sm:text-4xl">
          {article.title}
        </h1>

        <div className="mt-4 flex flex-wrap items-center gap-3 text-sm text-slate-400">
          <span className="flex items-center gap-1.5">
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-indigo-100 text-xs font-bold text-indigo-600">
              {article.author.nickname[0]}
            </span>
            <span className="text-slate-600">{article.author.nickname}</span>
          </span>
          <span>·</span>
          <span>{article.createTime}</span>
          <span>·</span>
          <span>{article.viewCount} 次阅读</span>
        </div>

        <div className="mt-4 flex flex-wrap gap-1.5">
          {article.tags.map((t) => (
            <span key={t.id} className="rounded-md bg-slate-100 px-2.5 py-0.5 text-xs text-slate-500">
              # {t.name}
            </span>
          ))}
        </div>
      </header>

      {/* 封面 */}
      {article.cover ? (
        <img src={article.cover} alt={article.title} className="mt-8 w-full rounded-2xl object-cover" />
      ) : (
        <div
          className={`mt-8 flex h-48 w-full items-center justify-center rounded-2xl bg-gradient-to-br ${coverGradient(
            article.category?.slug,
          )}`}
        >
          <span className="text-6xl font-bold text-white/90">{article.category?.name?.[0] ?? '文'}</span>
        </div>
      )}

      {/* 正文 */}
      <div className="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-10">
        <Markdown content={article.content} />
      </div>
    </div>
  )
}
