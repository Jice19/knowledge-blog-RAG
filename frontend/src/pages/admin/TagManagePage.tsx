import { useState } from 'react'
import { useData } from '../../store/DataContext'

export default function TagManagePage() {
  const { tags, addTag, deleteTag } = useData()
  const [name, setName] = useState('')

  const add = () => {
    if (!name.trim()) return
    addTag({ id: Date.now(), name: name.trim() })
    setName('')
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-slate-800">标签管理</h2>

      <div className="mt-4 flex gap-2">
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && add()}
          className="w-64 rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100"
          placeholder="新标签名称"
        />
        <button
          onClick={add}
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-700"
        >
          添加
        </button>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {tags.map((t) => (
          <span
            key={t.id}
            className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white py-1.5 pl-3.5 pr-1.5 text-sm text-slate-600 shadow-sm"
          >
            # {t.name}
            <button
              onClick={() => deleteTag(t.id)}
              className="flex h-5 w-5 items-center justify-center rounded-full bg-slate-100 text-xs text-slate-400 transition hover:bg-rose-50 hover:text-rose-500"
              title="删除"
            >
              ×
            </button>
          </span>
        ))}
      </div>
    </div>
  )
}
