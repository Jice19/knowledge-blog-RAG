export interface AskReference {
  articleId: number
  articleTitle: string
  heading: string
  score: number
}

export interface AskHandlers {
  onToken: (token: string) => void
  onReferences: (refs: AskReference[]) => void
  onDone: () => void
  onError: (err: unknown) => void
}

/**
 * SSE 流式问答：
 * 通过 EventSource 订阅 /api/rag/ask/stream，
 * 逐 token 回调 onToken，完成后回调 onReferences（引用来源）。
 */
export function askStream(q: string, topK: number, handlers: AskHandlers) {
  const es = new EventSource(
    `/api/rag/ask/stream?q=${encodeURIComponent(q)}&topK=${topK}`,
  )
  let finished = false

  es.addEventListener('token', (e) => {
    handlers.onToken((e as MessageEvent<string>).data)
  })
  es.addEventListener('references', (e) => {
    finished = true
    try {
      handlers.onReferences(JSON.parse((e as MessageEvent<string>).data))
    } catch {
      handlers.onReferences([])
    }
    handlers.onDone()
    es.close()
  })
  es.onerror = () => {
    if (!finished) {
      handlers.onError(new Error('连接中断，请重试'))
    }
    es.close()
  }
  return es
}
