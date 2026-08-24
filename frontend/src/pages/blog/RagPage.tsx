import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { askStream, type AskReference } from '../../api/rag'
import { createConversation, getMessages } from '../../api/conversation'
import Markdown from '../../components/Markdown'

interface Msg {
  role: 'user' | 'assistant'
  content: string
  references: AskReference[]
}

/** 旋转加载图标 */
function Spinner({ className = 'h-4 w-4' }: { className?: string }) {
  return (
    <svg className={`animate-spin ${className}`} viewBox="0 0 24 24" fill="none">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
    </svg>
  )
}

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
  const [messages, setMessages] = useState<Msg[]>([])
  const [conversationId, setConversationId] = useState<number | null>(
    () => Number(localStorage.getItem('akb_cid')) || null,
  )
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const esRef = useRef<EventSource | null>(null)
  const bottomRef = useRef<HTMLDivElement | null>(null)

  // 进入页面/切换会话时加载历史消息
  useEffect(() => {
    if (conversationId) {
      getMessages(conversationId)
        .then((list) =>
          setMessages(
            list.map((m) => ({
              role: m.role as 'user' | 'assistant',
              content: m.content,
              references: m.referencesJson ? JSON.parse(m.referencesJson) : [],
            })),
          ),
        )
        .catch(() => {})
    }
  }, [conversationId])

  // 新消息时滚动到底部
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const ask = async () => {
    const q = question.trim()
    if (!q || loading) return
    setError('')
    setMessages((prev) => [
      ...prev,
      { role: 'user', content: q, references: [] },
      { role: 'assistant', content: '', references: [] },
    ])
    setQuestion('')

    let cid = conversationId
    if (cid == null) {
      try {
        const c = await createConversation()
        cid = c.id
        setConversationId(c.id)
        localStorage.setItem('akb_cid', String(c.id))
      } catch {
        setError('创建会话失败')
        setLoading(false)
        return
      }
    }

    setLoading(true)
    esRef.current?.close()
    esRef.current = askStream(q, 3, cid, {
      onToken: (t) =>
        setMessages((prev) => {
          const next = [...prev]
          const last = next[next.length - 1]
          next[next.length - 1] = { ...last, content: last.content + t }
          return next
        }),
      onReferences: (refs) =>
        setMessages((prev) => {
          const next = [...prev]
          const last = next[next.length - 1]
          next[next.length - 1] = { ...last, references: refs }
          return next
        }),
      onDone: () => setLoading(false),
      onError: (e) => {
        setError(e instanceof Error ? e.message : '生成失败')
        setLoading(false)
      },
    })
  }

  const newConversation = () => {
    esRef.current?.close()
    setConversationId(null)
    setMessages([])
    setError('')
    setQuestion('')
    setLoading(false)
    localStorage.removeItem('akb_cid')
  }

  return (
    <div className="mx-auto flex h-[calc(100vh-4rem)] max-w-3xl flex-col px-4 py-6 sm:px-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">AI 知识问答</h1>
          <p className="mt-1 text-sm text-slate-500">基于全站文章的知识库问答（RAG · 支持多轮对话）</p>
        </div>
        <button
          onClick={newConversation}
          className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-600 transition hover:bg-slate-50"
        >
          ＋ 新建会话
        </button>
      </div>

      {error && <div className="mt-3 rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">{error}</div>}

      {/* 消息列表 */}
      <div className="mt-4 flex-1 space-y-4 overflow-y-auto pr-1">
        {messages.length === 0 && (
          <div className="py-20 text-center text-sm text-slate-400">
            输入问题开始对话，同一会话的历史会自动保留
          </div>
        )}
        {messages.map((m, i) =>
          m.role === 'user' ? (
            <div key={i} className="flex justify-end">
              <div className="max-w-[80%] whitespace-pre-wrap rounded-2xl rounded-br-md bg-indigo-600 px-4 py-2.5 text-sm leading-relaxed text-white">
                {m.content}
              </div>
            </div>
          ) : (
            <div key={i} className="flex justify-start">
              <div className="max-w-[88%]">
                <div className="rounded-2xl rounded-bl-md border border-slate-200 bg-white px-4 py-3 shadow-sm">
                  {m.content ? (
                    <Markdown content={m.content} />
                  ) : (
                    <div className="flex items-center gap-2 py-1 text-sm text-slate-500">
                      <Spinner className="h-4 w-4 text-indigo-500" />
                      <span>AI 正在思考</span>
                      <ThinkingDots />
                    </div>
                  )}
                </div>
                {m.references.length > 0 && (
                  <div className="mt-2 space-y-1">
                    {m.references.map((r, j) => (
                      <Link
                        key={j}
                        to={`/article/${r.articleId}`}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-500 transition hover:border-indigo-300"
                      >
                        <span className="rounded bg-indigo-50 px-1 py-0.5 text-[10px] font-medium text-indigo-600">
                          #{j + 1}
                        </span>
                        <span className="truncate">{r.articleTitle}</span>
                        {r.heading && <span className="shrink-0 text-slate-400">· {r.heading}</span>}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ),
        )}
        <div ref={bottomRef} />
      </div>

      {/* 输入区 */}
      <div className="mt-4 flex gap-2 border-t border-slate-200 pt-4">
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={(e) => {
            const ne = e.nativeEvent as KeyboardEvent
            if (e.key === 'Enter' && !ne.isComposing && ne.keyCode !== 229) ask()
          }}
          disabled={loading}
          placeholder="输入问题，回车发送；同一会话会记住上下文"
          className="flex-1 rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100 disabled:opacity-60"
        />
        <button
          onClick={ask}
          disabled={loading || !question.trim()}
          className="flex w-28 items-center justify-center gap-2 rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading ? <Spinner /> : '发送'}
        </button>
      </div>
    </div>
  )
}
