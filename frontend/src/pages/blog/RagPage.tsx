import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { askStream, type AskReference } from '../../api/rag'

export default function RagPage() {
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [references, setReferences] = useState<AskReference[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const esRef = useRef<EventSource | null>(null)

  const ask = () => {
    const q = question.trim()
    if (!q || loading) return
    setAnswer('')
    setReferences([])
    setError('')
    setLoading(true)
    esRef.current?.close()
    esRef.current = askStream(q, 3, {
      onToken: (t) => setAnswer((prev) => prev + t),
      onReferences: (refs) => setReferences(refs),
      onDone: () => setLoading(false),
      onError: (e) => {
        setError(e instanceof Error ? e.message : '生成失败')
        setLoading(false)
      },
    })
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <h1 className="text-2xl font-bold text-slate-900">AI 知识问答</h1>
      <p className="mt-1 text-sm text-slate-500">基于全站文章的知识库问答（RAG 检索增强生成）</p>

      <div className="mt-6 flex gap-2">
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && ask()}
          placeholder="输入你的问题，如：Redis 缓存有哪些经典问题？"
          className="flex-1 rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100"
        />
        <button
          onClick={ask}
          disabled={loading || !question.trim()}
          className="rounded-lg bg-indigo-600 px-5 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700 disabled:opacity-50"
        >
          {loading ? '思考中…' : '提问'}
        </button>
      </div>

      {error && <div className="mt-4 rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">{error}</div>}

      {(answer || loading) && (
        <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="whitespace-pre-wrap text-[15px] leading-relaxed text-slate-700">
            {answer}
            {loading && (
              <span className="ml-0.5 inline-block h-4 w-2 animate-pulse rounded-sm bg-indigo-500" />
            )}
          </div>
        </div>
      )}

      {references.length > 0 && (
        <div className="mt-6">
          <h2 className="text-sm font-semibold text-slate-500">引用来源</h2>
          <ul className="mt-3 space-y-2">
            {references.map((r, i) => (
              <li key={i}>
                <Link
                  to={`/article/${r.articleId}`}
                  className="flex items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm transition hover:border-indigo-300"
                >
                  <span className="flex min-w-0 items-center gap-2">
                    <span className="shrink-0 rounded bg-indigo-50 px-1.5 py-0.5 text-xs font-medium text-indigo-600">
                      #{i + 1}
                    </span>
                    <span className="truncate text-slate-700">{r.articleTitle}</span>
                    {r.heading && <span className="shrink-0 text-xs text-slate-400">· {r.heading}</span>}
                  </span>
                  <span className="ml-3 shrink-0 text-xs text-slate-400">{r.score.toFixed(2)}</span>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
