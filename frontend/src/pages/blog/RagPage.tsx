import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { askStream, type AskReference } from '../../api/rag'
import {
  createConversation,
  deleteConversation,
  getMessages,
  listConversations,
  type Conversation,
} from '../../api/conversation'
import Markdown from '../../components/Markdown'

interface Msg {
  role: 'user' | 'assistant'
  content: string
  references: AskReference[]
}

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
  const [convList, setConvList] = useState<Conversation[]>([])
  const [conversationId, setConversationId] = useState<number | null>(
    () => Number(localStorage.getItem('akb_cid')) || null,
  )
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const esRef = useRef<EventSource | null>(null)
  const bottomRef = useRef<HTMLDivElement | null>(null)

  const loadConvList = () => {
    listConversations()
      .then(setConvList)
      .catch(() => {})
  }

  useEffect(() => {
    loadConvList()
  }, [])

  // 切换/进入会话时加载历史消息
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

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const selectConversation = (id: number) => {
    esRef.current?.close()
    setConversationId(id)
    localStorage.setItem('akb_cid', String(id))
  }

  const removeConversation = async (id: number) => {
    if (!window.confirm('确定删除该会话吗？')) return
    try {
      await deleteConversation(id)
      if (conversationId === id) {
        setConversationId(null)
        setMessages([])
        localStorage.removeItem('akb_cid')
      }
      loadConvList()
    } catch {
      /* ignore */
    }
  }

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
        loadConvList()
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
      onDone: () => {
        setLoading(false)
        loadConvList() // 刷新标题
      },
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
    <div className="mx-auto flex h-[calc(100vh-4rem)] max-w-5xl px-4 py-6 sm:px-6">
      {/* 侧边栏：会话列表 */}
      <aside className="mr-4 hidden w-60 shrink-0 flex-col rounded-2xl border border-slate-200 bg-white shadow-sm sm:flex">
        <div className="border-b border-slate-100 p-3">
          <button
            onClick={newConversation}
            className="flex w-full items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-3 py-2.5 text-sm font-medium text-white shadow-md transition hover:opacity-90"
          >
            <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M12 5v14M5 12h14" strokeLinecap="round" />
            </svg>
            新建会话
          </button>
        </div>
        <div className="flex-1 space-y-1 overflow-y-auto p-2">
          <div className="px-2 py-1 text-xs font-medium text-slate-400">历史会话</div>
          {convList.length === 0 && (
            <div className="py-8 text-center text-xs text-slate-400">暂无会话，开始提问吧</div>
          )}
          {convList.map((c) => (
            <div
              key={c.id}
              onClick={() => selectConversation(c.id)}
              className={`group flex cursor-pointer items-center gap-2 rounded-xl px-3 py-2.5 text-sm transition ${
                conversationId === c.id
                  ? 'bg-indigo-50 font-medium text-indigo-600'
                  : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              <span className="shrink-0 text-xs">💬</span>
              <span className="flex-1 truncate">{c.title}</span>
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  removeConversation(c.id)
                }}
                className="hidden shrink-0 rounded px-1 text-xs text-slate-400 hover:text-rose-500 group-hover:block"
                title="删除会话"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      </aside>

      {/* 聊天区 */}
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold text-slate-900">AI 知识问答</h1>
          <button
            onClick={newConversation}
            className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-600 transition hover:bg-slate-50 sm:hidden"
          >
            ＋ 新建会话
          </button>
        </div>

        {error && <div className="mt-3 rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">{error}</div>}

        <div className="mt-4 flex-1 space-y-4 overflow-y-auto pr-1">
          {messages.length === 0 && (
            <div className="flex h-full flex-col items-center justify-center py-10 text-center">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-violet-600 text-3xl shadow-lg">
                🤖
              </div>
              <h2 className="mt-4 text-lg font-semibold text-slate-700">有什么想问的？</h2>
              <p className="mt-1 text-sm text-slate-400">基于 300+ 篇文章的知识库问答 · 支持多轮对话</p>
              <div className="mt-6 flex w-full max-w-md flex-col gap-2">
                {['Redis 缓存有哪些经典问题？', 'Spring Boot 3 和 Java 17 有什么关系？', 'Nginx 反向代理怎么配置？'].map(
                  (s) => (
                    <button
                      key={s}
                      onClick={() => setQuestion(s)}
                      className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-left text-sm text-slate-600 shadow-sm transition hover:border-indigo-300 hover:text-indigo-600"
                    >
                      {s}
                    </button>
                  ),
                )}
              </div>
            </div>
          )}
          {messages.map((m, i) =>
            m.role === 'user' ? (
              <div key={i} className="flex justify-end">
                <div className="max-w-[75%] whitespace-pre-wrap rounded-2xl rounded-br-md bg-gradient-to-br from-indigo-500 to-indigo-600 px-4 py-2.5 text-sm leading-relaxed text-white shadow-sm">
                  {m.content}
                </div>
              </div>
            ) : (
              <div key={i} className="flex justify-start gap-2">
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 text-[10px] font-bold text-white shadow-sm">
                  AI
                </div>
                <div className="max-w-[80%]">
                  <div className="rounded-2xl rounded-tl-md border border-slate-200 bg-white px-4 py-3 shadow-sm">
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
                          className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-500 shadow-sm transition hover:border-indigo-300"
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

        <div className="mt-4 flex items-center gap-2 border-t border-slate-200 pt-4">
          <input
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={(e) => {
              const ne = e.nativeEvent as KeyboardEvent
              if (e.key === 'Enter' && !ne.isComposing && ne.keyCode !== 229) ask()
            }}
            disabled={loading}
            placeholder="输入问题，回车发送；同一会话会记住上下文"
            className="flex-1 rounded-full border border-slate-200 bg-white px-4 py-2.5 text-sm shadow-sm outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100 disabled:opacity-60"
          />
          <button
            onClick={ask}
            disabled={loading || !question.trim()}
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-indigo-600 to-violet-600 text-white shadow-md transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
            title="发送"
          >
            {loading ? (
              <Spinner />
            ) : (
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M3.4 20.4l17.45-7.48a1 1 0 0 0 0-1.84L3.4 3.6a.993.993 0 0 0-1.39.91L2 9.12c0 .5.37.93.87.99L17 12 2.87 13.88c-.5.07-.87.5-.87 1l.01 4.61c0 .71.73 1.2 1.39.91z" />
              </svg>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
