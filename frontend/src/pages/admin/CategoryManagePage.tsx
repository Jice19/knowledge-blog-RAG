import { useState } from 'react'
import { useData } from '../../store/DataContext'

export default function CategoryManagePage() {
  const { categories, addCategory, deleteCategory } = useData()
  const [name, setName] = useState('')

  const add = () => {
    if (!name.trim()) return
    addCategory({
      id: Date.now(),
      name: name.trim(),
      slug: `cat-${Date.now()}`,
      sort: categories.length + 1,
    })
    setName('')
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-slate-800">分类管理</h2>

      <div className="mt-4 flex gap-2">
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && add()}
          className="w-64 rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100"
          placeholder="新分类名称"
        />
        <button
          onClick={add}
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-700"
        >
          添加
        </button>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {categories.map((c) => (
          <div
            key={c.id}
            className="flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
          >
            <div>
              <div className="font-medium text-slate-800">{c.name}</div>
              <div className="mt-0.5 text-xs text-slate-400">/{c.slug}</div>
            </div>
            <button onClick={() => deleteCategory(c.id)} className="text-sm text-rose-500 transition hover:text-rose-600">
              删除
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
