import { Link } from 'react-router-dom'
import type { Article } from '../types'
import { coverGradient } from '../utils/cover'
import StatusBadge from './StatusBadge'

export default function ArticleCard({ article }: { article: Article }) {
  const slug = article.category?.slug
  return (
    <Link
      to={`/article/${article.id}`}
      className="group flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-lg"
    >
      {article.cover ? (
        <img src={article.cover} alt={article.title} className="h-44 w-full object-cover" />
      ) : (
        <div
          className={`flex h-44 w-full items-center justify-center bg-gradient-to-br ${coverGradient(slug)}`}
        >
          <span className="text-5xl font-bold text-white/90">
            {article.category?.name?.[0] ?? '文'}
          </span>
        </div>
      )}

      <div className="flex flex-1 flex-col p-5">
        <div className="flex items-center gap-2 text-xs">
          {article.category && (
            <span className="rounded-full bg-indigo-50 px-2.5 py-0.5 font-medium text-indigo-600">
              {article.category.name}
            </span>
          )}
          {article.status === 0 && <StatusBadge status={article.status} />}
        </div>

        <h3 className="mt-3 line-clamp-2 text-lg font-semibold leading-snug text-slate-900 transition-colors group-hover:text-indigo-600">
          {article.title}
        </h3>
        <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-slate-500">{article.summary}</p>

        <div className="mt-4 flex flex-wrap gap-1.5">
          {article.tags.slice(0, 3).map((t) => (
            <span key={t.id} className="rounded-md bg-slate-100 px-2 py-0.5 text-xs text-slate-500">
              {t.name}
            </span>
          ))}
        </div>

        <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-400">
          <span className="flex items-center gap-1.5">
            <span className="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-100 text-[10px] font-bold text-indigo-600">
              {article.author.nickname[0]}
            </span>
            {article.author.nickname}
          </span>
          <span className="flex items-center gap-3">
            <span>{article.createTime}</span>
            <span className="flex items-center gap-1">
              <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              {article.viewCount}
            </span>
          </span>
        </div>
      </div>
    </Link>
  )
}
