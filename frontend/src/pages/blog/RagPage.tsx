import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { askStream, type AskReference } from '../../api/rag'

/** 旋转的加载图标 */
function Spinner({ className = 'h-4 w-4' }: { className?: string }) {
  return (
    <svg className={`animate-spin ${className}`} viewBox="0 0 24 24" fill="none">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
    </svg>
  )
}

/** 思考中的跳动小圆点 */
function ThinkingDots() {
  return (
    <span className="inline-flex gap-0.5">
      <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-indigo-400" />
      <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-indigo-400 [animation-delay:150ms]" />
      <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-indigo-400 [animation-delay:300ms]" />
    </span>
  )
}

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

      {/* 输入区 */}
      <div className="mt-6 flex gap-2">
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && ask()}
          disabled={loading}
          placeholder="输入你的问题，如：Redis 缓存有哪些经典问题？"
          className="flex-1 rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100 disabled:opacity-60"
        />
        <button
          onClick={ask}
          disabled={loading || !question.trim()}
          className="flex w-32 items-center justify-center gap-2 rounded-lg bg-indigo-600 px-5 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading ? (
            <>
              <Spinner />
              思考中…
            </>
          ) : (
            '提问'
          )}
        </button>
      </div>

      {error && <div className="mt-4 rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">{error}</div>}

      {/* 思考阶段：还没收到第一个字 */}
      {loading && !answer && (
        <div className="mt-6 flex items-center gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-indigo-100">
            <Spinner className="h-5 w-5 text-indigo-600" />
          </div>
          <div className="text-sm text-slate-500">
            <span className="font-medium text-slate-700">AI 正在检索知识库并思考</span>
            <ThinkingDots />
            <p className="mt-0.5 text-xs text-slate-400">本机模型推理中，首次可能需要十几秒</p>
          </div>
        </div>
      )}

      {/* 流式/完成阶段：有答案了 */}
      {(answer || (!loading && error)) && (
        <div className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center gap-2 border-b border-slate-100 px-6 py-3">
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-indigo-600 text-[10px] font-bold text-white">
              AI
            </span>
            <span className="text-sm font-medium text-slate-600">回答</span>
            {loading && <Spinner className="ml-auto h-4 w-4 text-indigo-500" />}
          </div>
          <div className="whitespace-pre-wrap px-6 py-5 text-[15px] leading-relaxed text-slate-700">
            {answer}
            {loading && (
              <span className="ml-0.5 inline-block h-4 w-2 animate-pulse rounded-sm bg-indigo-500 align-middle" />
            )}
          </div>
        </div>
      )}

      {/* 引用来源 */}
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
