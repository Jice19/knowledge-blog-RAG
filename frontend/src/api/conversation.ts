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

/** 会话列表 */
export function listConversations() {
  return http.get('/rag/conversations') as Promise<Conversation[]>
}

/** 删除会话 */
export function deleteConversation(id: number) {
  return http.delete(`/rag/conversations/${id}`)
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
