import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  adminPageArticles,
  deleteArticle,
  deleteBatchArticles,
  updateArticle,
  type ArticleDTO,
  type ArticleVO,
} from '../../api/article'
import StatusBadge from '../../components/StatusBadge'

function VectorBadge({ status }: { status?: number }) {
  const map: Record<number, { text: string; cls: string }> = {
    0: { text: '待入库', cls: 'bg-slate-100 text-slate-500' },
    1: { text: '入库中', cls: 'bg-amber-100 text-amber-600' },
    2: { text: '已入库', cls: 'bg-emerald-100 text-emerald-600' },
    3: { text: '失败', cls: 'bg-rose-100 text-rose-600' },
  }
  const s = map[status ?? 0] ?? map[0]
  return <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${s.cls}`}>{s.text}</span>
}

export default function ArticleListPage() {
  const [list, setList] = useState<ArticleVO[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size] = useState(10)
  const [status, setStatus] = useState<number | undefined>(undefined)
  const [loading, setLoading] = useState(false)
  const [selected, setSelected] = useState<Set<number>>(new Set())

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const data = await adminPageArticles({ page, size, status })
      setList(data.records)
      setTotal(data.total)
    } catch {
      setList([])
    } finally {
      setLoading(false)
    }
  }, [page, size, status])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const totalPages = Math.max(1, Math.ceil(total / size))

  const toggleStatus = async (a: ArticleVO) => {
    const payload: ArticleDTO = {
      title: a.title,
      summary: a.summary,
      content: a.content,
      cover: a.cover,
      categoryId: a.categoryId,
      tagIds: a.tags.map((t) => t.id),
      status: a.status === 1 ? 0 : 1,
    }
    await updateArticle(a.id, payload)
    fetchData()
  }

  const remove = async (id: number, title: string) => {
    if (!window.confirm(`确定删除文章「${title}」吗？删除后将停止其向量化入库。`)) return
    await deleteArticle(id)
    fetchData()
  }

  const allSelected = list.length > 0 && list.every((a) => selected.has(a.id))

  const toggleAll = () => {
    setSelected(allSelected ? new Set() : new Set(list.map((a) => a.id)))
  }

  const toggleOne = (id: number) => {
    const next = new Set(selected)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    setSelected(next)
  }

  const batchRemove = async () => {
    if (selected.size === 0) return
    if (!window.confirm(`确定删除选中的 ${selected.size} 篇文章吗？删除后将停止其向量化入库，不可恢复。`)) return
    try {
      await deleteBatchArticles([...selected])
      setSelected(new Set())
      fetchData()
    } catch {
      // 失败保持选中，可重试
    }
  }

  const filterBtn = (v: number | undefined, label: string) =>
    `rounded-lg px-3 py-1.5 text-sm font-medium transition ${
      status === v ? 'bg-indigo-600 text-white' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
    }`

  return (
    <div>
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-800">文章管理</h2>
        <div className="flex items-center gap-2">
          <button
            onClick={batchRemove}
            disabled={selected.size === 0}
            className="rounded-lg border border-rose-200 px-4 py-2 text-sm font-medium text-rose-600 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            批量删除{selected.size > 0 ? `（${selected.size}）` : ''}
          </button>
          <Link
            to="/admin/articles/new"
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700"
          >
            + 新建文章
          </Link>
        </div>
      </div>

      <div className="mt-4 flex gap-2">
        <button onClick={() => { setStatus(undefined); setPage(1) }} className={filterBtn(undefined, '全部')}>
          全部
        </button>
        <button onClick={() => { setStatus(1); setPage(1) }} className={filterBtn(1, '已发布')}>
          已发布
        </button>
        <button onClick={() => { setStatus(0); setPage(1) }} className={filterBtn(0, '草稿')}>
          草稿
        </button>
      </div>

      <div className="mt-4 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs text-slate-500">
            <tr>
              <th className="w-10 px-4 py-3">
                <input
                  type="checkbox"
                  checked={allSelected}
                  onChange={toggleAll}
                  className="h-4 w-4 rounded border-slate-300 text-indigo-600"
                />
              </th>
              <th className="px-5 py-3 font-medium">标题</th>
              <th className="px-4 py-3 font-medium">分类</th>
              <th className="px-4 py-3 font-medium">状态</th>
              <th className="px-4 py-3 font-medium">入库</th>
              <th className="px-4 py-3 font-medium">浏览</th>
              <th className="px-4 py-3 font-medium">日期</th>
              <th className="px-5 py-3 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {list.map((a) => (
              <tr key={a.id} className="transition hover:bg-slate-50">
                <td className="px-4 py-3">
                  <input
                    type="checkbox"
                    checked={selected.has(a.id)}
                    onChange={() => toggleOne(a.id)}
                    className="h-4 w-4 rounded border-slate-300 text-indigo-600"
                  />
                </td>
                <td className="max-w-xs px-5 py-3">
                  <div className="truncate font-medium text-slate-800">{a.title}</div>
                </td>
                <td className="px-4 py-3 text-slate-500">{a.categoryName ?? '-'}</td>
                <td className="px-4 py-3">
                  <StatusBadge status={a.status} />
                </td>
                <td className="px-4 py-3">
                  <VectorBadge status={a.vectorStatus} />
                </td>
                <td className="px-4 py-3 text-slate-500">{a.viewCount}</td>
                <td className="px-4 py-3 text-slate-500">{a.createTime?.slice(0, 10)}</td>
                <td className="px-5 py-3">
                  <div className="flex justify-end gap-3">
                    <Link to={`/admin/articles/${a.id}/edit`} className="text-indigo-600 transition hover:text-indigo-700">
                      编辑
                    </Link>
                    <button onClick={() => toggleStatus(a)} className="text-slate-500 transition hover:text-slate-700">
                      {a.status === 1 ? '下线' : '发布'}
                    </button>
                    <button onClick={() => remove(a.id, a.title)} className="text-rose-500 transition hover:text-rose-600">
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {list.length === 0 && !loading && (
          <div className="py-16 text-center text-slate-400">暂无文章，点击右上角新建</div>
        )}
      </div>

      <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
        <span>共 {total} 条</span>
        <div className="flex items-center gap-3">
          <button
            disabled={page <= 1}
            onClick={() => setPage(page - 1)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 transition hover:bg-slate-50 disabled:opacity-40"
          >
            上一页
          </button>
          <span>
            {page} / {totalPages}
          </span>
          <button
            disabled={page >= totalPages}
            onClick={() => setPage(page + 1)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 transition hover:bg-slate-50 disabled:opacity-40"
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  )
}
