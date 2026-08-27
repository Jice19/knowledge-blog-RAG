import { useRef, useState } from 'react'
import { uploadNotes } from '../../api/notes'

export default function NotesImportPage() {
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<{ created: number; skipped: number; titles: string[] } | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const submit = async () => {
    if (!file) return
    setUploading(true)
    setError('')
    setResult(null)
    try {
      setResult(await uploadNotes(file))
      if (inputRef.current) inputRef.current.value = ''
      setFile(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : '上传失败')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-slate-800">笔记导入</h2>
      <p className="mt-1 text-sm text-slate-500">
        上传 Markdown 笔记，系统按「# 一级标题」自动拆成多篇文章、推断分类并发布，随后异步向量化入库。
      </p>

      <div className="mt-4 rounded-xl border border-dashed border-slate-300 bg-white p-6">
        <input
          ref={inputRef}
          type="file"
          accept=".md,.markdown,text/markdown"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          className="block w-full text-sm text-slate-600 file:mr-4 file:rounded-lg file:border-0 file:bg-indigo-50 file:px-4 file:py-2 file:text-sm file:font-medium file:text-indigo-600 hover:file:bg-indigo-100"
        />
        <div className="mt-4 flex items-center gap-3">
          <button
            onClick={submit}
            disabled={!file || uploading}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {uploading ? '上传中…' : '开始导入'}
          </button>
          {file && <span className="text-sm text-slate-500">已选择：{file.name}</span>}
        </div>
      </div>

      {error && <div className="mt-3 rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">{error}</div>}

      {result && (
        <div className="mt-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="text-sm text-slate-700">
            导入完成：<span className="font-semibold text-indigo-600">{result.created}</span> 篇已发布
            {result.skipped > 0 && <span>，跳过 {result.skipped} 篇空内容</span>}
          </div>
          <div className="mt-3 max-h-80 overflow-auto">
            {result.titles.map((t) => (
              <div key={t} className="border-b border-slate-100 py-1.5 text-sm text-slate-600">
                {t}
              </div>
            ))}
          </div>
          <p className="mt-3 text-xs text-slate-400">文章已进入异步向量化队列，稍后即可在问答中检索到。</p>
        </div>
      )}
    </div>
  )
}
