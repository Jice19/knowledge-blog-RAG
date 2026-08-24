import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminPageArticles, type ArticleVO } from '../../api/article'

export default function DashboardPage() {
  const [articles, setArticles] = useState<ArticleVO[]>([])

  useEffect(() => {
    adminPageArticles({ page: 1, size: 100 })
      .then((d) => setArticles(d.records))
      .catch(() => {})
  }, [])

  const published = articles.filter((a) => a.status === 1)
  const drafts = articles.filter((a) => a.status === 0)
  const totalViews = articles.reduce((s, a) => s + a.viewCount, 0)

  const stats = [
    { label: '文章总数', value: articles.length, color: 'text-indigo-600', icon: '📄' },
    { label: '已发布', value: published.length, color: 'text-emerald-600', icon: '✅' },
    { label: '草稿', value: drafts.length, color: 'text-amber-600', icon: '📝' },
    { label: '总浏览量', value: totalViews, color: 'text-sky-600', icon: '👁️' },
  ]

  return (
    <div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <div key={s.label} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-500">{s.label}</span>
              <span className="text-lg">{s.icon}</span>
            </div>
            <div className={`mt-2 text-3xl font-bold ${s.color}`}>{s.value}</div>
          </div>
        ))}
      </div>

      <div className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <h2 className="font-semibold text-slate-800">最近文章</h2>
          <Link to="/admin/articles" className="text-sm text-indigo-600 transition hover:text-indigo-700">
            查看全部 →
          </Link>
        </div>
        <ul className="divide-y divide-slate-100">
          {articles.slice(0, 5).map((a) => (
            <li key={a.id} className="flex items-center justify-between px-5 py-3">
              <span className="truncate text-sm text-slate-700">{a.title}</span>
              <span className="ml-4 shrink-0 text-xs text-slate-400">{a.createTime?.slice(0, 10)}</span>
            </li>
          ))}
          {articles.length === 0 && <li className="px-5 py-8 text-center text-sm text-slate-400">暂无文章</li>}
        </ul>
      </div>
    </div>
  )
}
