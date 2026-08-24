import { Link } from 'react-router-dom'
import { useData } from '../../store/DataContext'
import StatusBadge from '../../components/StatusBadge'
import type { Article } from '../../types'

export default function ArticleListPage() {
  const { articles, updateArticle, deleteArticle } = useData()

  const toggleStatus = (article: Article) => {
    updateArticle({ ...article, status: article.status === 1 ? 0 : 1 })
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-800">文章管理</h2>
        <Link
          to="/admin/articles/new"
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700"
        >
          + 新建文章
        </Link>
      </div>

      <div className="mt-4 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs text-slate-500">
            <tr>
              <th className="px-5 py-3 font-medium">标题</th>
              <th className="px-4 py-3 font-medium">分类</th>
              <th className="px-4 py-3 font-medium">状态</th>
              <th className="px-4 py-3 font-medium">浏览</th>
              <th className="px-4 py-3 font-medium">日期</th>
              <th className="px-5 py-3 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {articles.map((a) => (
              <tr key={a.id} className="transition hover:bg-slate-50">
                <td className="max-w-xs px-5 py-3">
                  <div className="truncate font-medium text-slate-800">{a.title}</div>
                </td>
                <td className="px-4 py-3 text-slate-500">{a.category?.name ?? '-'}</td>
                <td className="px-4 py-3">
                  <StatusBadge status={a.status} />
                </td>
                <td className="px-4 py-3 text-slate-500">{a.viewCount}</td>
                <td className="px-4 py-3 text-slate-500">{a.createTime}</td>
                <td className="px-5 py-3">
                  <div className="flex justify-end gap-3">
                    <Link to={`/admin/articles/${a.id}/edit`} className="text-indigo-600 transition hover:text-indigo-700">
                      编辑
                    </Link>
                    <button onClick={() => toggleStatus(a)} className="text-slate-500 transition hover:text-slate-700">
                      {a.status === 1 ? '下线' : '发布'}
                    </button>
                    <button onClick={() => deleteArticle(a.id)} className="text-rose-500 transition hover:text-rose-600">
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {articles.length === 0 && (
          <div className="py-16 text-center text-slate-400">暂无文章，点击右上角新建</div>
        )}
      </div>
    </div>
  )
}
