import http from './http'

export interface Conversation {
  id: number
  userId: number
  title: string
  createTime: string
  updateTime: string
}

/** 新建会话 */
export function createConversation(title?: string) {
  return http.post('/rag/conversations', undefined, { params: title ? { title } : {} }) as Promise<Conversation>
}

export interface ChatMessage {
  id: number
  conversationId: number
  role: string
  content: string
  referencesJson: string | null
  createTime: string
}

/** 获取某会话的全部消息 */
export function getMessages(conversationId: number) {
  return http.get(`/rag/conversations/${conversationId}/messages`) as Promise<ChatMessage[]>
}
