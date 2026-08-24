import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useData } from '../../store/DataContext'
import Markdown from '../../components/Markdown'
import { mockUser } from '../../data/mock'
import type { Article, ArticleStatus } from '../../types'

export default function ArticleEditPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { articles, categories, tags, addArticle, updateArticle } = useData()
  const editing = id ? articles.find((a) => a.id === Number(id)) : undefined

  const [title, setTitle] = useState(editing?.title ?? '')
  const [summary, setSummary] = useState(editing?.summary ?? '')
  const [content, setContent] = useState(editing?.content ?? '')
  const [cover, setCover] = useState(editing?.cover ?? '')
  const [categoryId, setCategoryId] = useState(editing?.categoryId ?? categories[0]?.id ?? 0)
  const [tagIds, setTagIds] = useState<number[]>(editing?.tags.map((t) => t.id) ?? [])
  const [status, setStatus] = useState<ArticleStatus>(editing?.status ?? 0)
  const [preview, setPreview] = useState(false)

  const toggleTag = (tid: number) =>
    setTagIds((prev) => (prev.includes(tid) ? prev.filter((x) => x !== tid) : [...prev, tid]))

  const save = () => {
    if (!title.trim()) return
    const article: Article = {
      id: editing?.id ?? Date.now(),
      title: title.trim(),
      summary,
      content,
      cover,
      categoryId,
      category: categories.find((c) => c.id === categoryId),
      tags: tags.filter((t) => tagIds.includes(t.id)),
      author: mockUser,
      status,
      viewCount: editing?.viewCount ?? 0,
      createTime: editing?.createTime ?? new Date().toISOString().slice(0, 10),
    }
    if (editing) updateArticle(article)
    else addArticle(article)
    navigate('/admin/articles')
  }

  const inputCls =
    'mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100'

  return (
    <div>
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-800">{editing ? '编辑文章' : '新建文章'}</h2>
        <div className="flex gap-2">
          <button
            onClick={() => navigate('/admin/articles')}
            className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-50"
          >
            取消
          </button>
          <button
            onClick={save}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700"
          >
            保存
          </button>
        </div>
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-3">
        {/* 元信息表单 */}
        <div className="space-y-4 lg:col-span-1">
          <div className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div>
              <label className="text-sm font-medium text-slate-600">标题</label>
              <input value={title} onChange={(e) => setTitle(e.target.value)} className={inputCls} placeholder="文章标题" />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-600">分类</label>
              <select value={categoryId} onChange={(e) => setCategoryId(Number(e.target.value))} className={inputCls}>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-sm font-medium text-slate-600">标签</label>
              <div className="mt-1.5 flex flex-wrap gap-1.5">
                {tags.map((t) => (
                  <button
                    key={t.id}
                    type="button"
                    onClick={() => toggleTag(t.id)}
                    className={`rounded-full px-3 py-1 text-xs font-medium transition ${
                      tagIds.includes(t.id)
                        ? 'bg-indigo-600 text-white'
                        : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
                    }`}
                  >
                    {t.name}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="text-sm font-medium text-slate-600">摘要</label>
              <textarea
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                rows={3}
                className={inputCls}
                placeholder="文章摘要（列表页展示）"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-600">封面 URL</label>
              <input
                value={cover}
                onChange={(e) => setCover(e.target.value)}
                className={inputCls}
                placeholder="留空则使用分类渐变占位"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-600">状态</label>
              <div className="mt-1.5 flex gap-2">
                <button
                  type="button"
                  onClick={() => setStatus(0)}
                  className={`rounded-lg px-4 py-1.5 text-sm font-medium transition ${
                    status === 0 ? 'bg-amber-500 text-white' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
                  }`}
                >
                  草稿
                </button>
                <button
                  type="button"
                  onClick={() => setStatus(1)}
                  className={`rounded-lg px-4 py-1.5 text-sm font-medium transition ${
                    status === 1 ? 'bg-emerald-500 text-white' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
                  }`}
                >
                  发布
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Markdown 编辑器 */}
        <div className="lg:col-span-2">
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex border-b border-slate-100">
              <button
                onClick={() => setPreview(false)}
                className={`px-5 py-3 text-sm font-medium transition ${
                  !preview ? 'border-b-2 border-indigo-600 text-indigo-600' : 'text-slate-500 hover:text-slate-700'
                }`}
              >
                编辑
              </button>
              <button
                onClick={() => setPreview(true)}
                className={`px-5 py-3 text-sm font-medium transition ${
                  preview ? 'border-b-2 border-indigo-600 text-indigo-600' : 'text-slate-500 hover:text-slate-700'
                }`}
              >
                预览
              </button>
            </div>

            {preview ? (
              <div className="max-h-[600px] overflow-auto p-6">
                {content ? <Markdown content={content} /> : <p className="text-sm text-slate-400">暂无内容</p>}
              </div>
            ) : (
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                className="min-h-[600px] w-full resize-y p-6 font-mono text-sm leading-relaxed text-slate-700 outline-none"
                placeholder="支持 Markdown 语法：# 标题、```代码块```、- 列表、> 引用…"
              />
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
