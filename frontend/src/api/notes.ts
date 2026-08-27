import http from './http'

export interface NotesImportResult {
  created: number
  skipped: number
  titles: string[]
}

/** 上传 Markdown 笔记，后端自动按 # 拆分并发布 */
export function uploadNotes(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/admin/notes/import', fd) as Promise<NotesImportResult>
}
