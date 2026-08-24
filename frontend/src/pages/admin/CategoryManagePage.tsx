import { useCallback, useEffect, useState } from 'react'
import { createCategory, deleteCategory, listCategories, type Category } from '../../api/category'

export default function CategoryManagePage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [name, setName] = useState('')
  const [error, setError] = useState('')

  const fetchData = useCallback(() => {
    listCategories()
      .then(setCategories)
      .catch(() => {})
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const add = async () => {
    if (!name.trim()) return
    setError('')
    try {
      await createCategory({ name: name.trim(), slug: `cat-${Date.now()}`, sort: categories.length + 1 })
      setName('')
      fetchData()
    } catch (e) {
      setError(e instanceof Error ? e.message : '添加失败')
    }
  }

  const remove = async (id: number, name: string) => {
    if (!window.confirm(`确定删除分类「${name}」吗？`)) return
    try {
      await deleteCategory(id)
      fetchData()
    } catch (e) {
      setError(e instanceof Error ? e.message : '删除失败')
    }
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-slate-800">分类管理</h2>

      <div className="mt-4 flex gap-2">
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => {
            const ne = e.nativeEvent as KeyboardEvent
            if (e.key === 'Enter' && !ne.isComposing && ne.keyCode !== 229) add()
          }}
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

      {error && <div className="mt-3 rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">{error}</div>}

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
            <button onClick={() => remove(c.id, c.name)} className="text-sm text-rose-500 transition hover:text-rose-600">
              删除
            </button>
          </div>
        ))}
        {categories.length === 0 && <div className="col-span-full py-10 text-center text-sm text-slate-400">暂无分类</div>}
      </div>
    </div>
  )
}
