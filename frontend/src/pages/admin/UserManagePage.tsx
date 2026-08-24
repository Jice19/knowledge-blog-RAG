import { useCallback, useEffect, useState } from 'react'
import {
  createUser,
  deleteUser,
  pageUsers,
  updateUser,
  type UserDTO,
  type UserVO,
} from '../../api/user'

const emptyForm: UserDTO = { username: '', password: '', nickname: '', role: 'USER' }

const inputCls =
  'w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100'

export default function UserManagePage() {
  const [list, setList] = useState<UserVO[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<UserDTO>(emptyForm)

  const fetchData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await pageUsers({ page, size, keyword: keyword || undefined })
      setList(data.records)
      setTotal(data.total)
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }, [page, size, keyword])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const totalPages = Math.max(1, Math.ceil(total / size))

  const openCreate = () => {
    setEditingId(null)
    setForm(emptyForm)
    setError('')
    setModalOpen(true)
  }

  const openEdit = (u: UserVO) => {
    setEditingId(u.id)
    setForm({ username: u.username, password: '', nickname: u.nickname ?? '', role: u.role })
    setError('')
    setModalOpen(true)
  }

  const submit = async () => {
    if (!form.username.trim()) return
    try {
      if (editingId) await updateUser(editingId, form)
      else await createUser(form)
      setModalOpen(false)
      fetchData()
    } catch (e) {
      setError(e instanceof Error ? e.message : '保存失败')
    }
  }

  const remove = async (id: number, username: string) => {
    if (!window.confirm(`确定删除用户「${username}」吗？`)) return
    try {
      await deleteUser(id)
      fetchData()
    } catch (e) {
      setError(e instanceof Error ? e.message : '删除失败')
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-800">用户管理</h2>
        <button
          onClick={openCreate}
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700"
        >
          + 新增用户
        </button>
      </div>

      <div className="mt-4 flex gap-2">
        <input
          value={keyword}
          onChange={(e) => {
            setKeyword(e.target.value)
            setPage(1)
          }}
          onKeyDown={(e) => e.key === 'Enter' && fetchData()}
          className="w-64 rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-indigo-400"
          placeholder="搜索用户名 / 昵称"
        />
        <button
          onClick={() => {
            setPage(1)
            fetchData()
          }}
          className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-50"
        >
          搜索
        </button>
      </div>

      {error && <div className="mt-3 rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">{error}</div>}

      <div className="mt-4 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs text-slate-500">
            <tr>
              <th className="px-5 py-3 font-medium">ID</th>
              <th className="px-4 py-3 font-medium">用户名</th>
              <th className="px-4 py-3 font-medium">昵称</th>
              <th className="px-4 py-3 font-medium">角色</th>
              <th className="px-4 py-3 font-medium">创建时间</th>
              <th className="px-5 py-3 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {list.map((u) => (
              <tr key={u.id} className="transition hover:bg-slate-50">
                <td className="px-5 py-3 text-slate-500">{u.id}</td>
                <td className="px-4 py-3 font-medium text-slate-800">{u.username}</td>
                <td className="px-4 py-3 text-slate-600">{u.nickname || '-'}</td>
                <td className="px-4 py-3">
                  <span
                    className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
                      u.role === 'ADMIN' ? 'bg-indigo-50 text-indigo-600' : 'bg-slate-100 text-slate-500'
                    }`}
                  >
                    {u.role}
                  </span>
                </td>
                <td className="px-4 py-3 text-slate-500">
                  {u.createTime ? u.createTime.replace('T', ' ').slice(0, 19) : '-'}
                </td>
                <td className="px-5 py-3">
                  <div className="flex justify-end gap-3">
                    <button onClick={() => openEdit(u)} className="text-indigo-600 transition hover:text-indigo-700">
                      编辑
                    </button>
                    <button
                      onClick={() => remove(u.id, u.username)}
                      className="text-rose-500 transition hover:text-rose-600"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {list.length === 0 && !loading && (
          <div className="py-16 text-center text-slate-400">暂无用户</div>
        )}
      </div>

      <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
        <span>共 {total} 条</span>
        <div className="flex items-center gap-3">
          <button
            disabled={page <= 1}
            onClick={() => setPage(page - 1)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 transition hover:bg-slate-50 disabled:opacity-40"
          >
            上一页
          </button>
          <span>
            {page} / {totalPages}
          </span>
          <button
            disabled={page >= totalPages}
            onClick={() => setPage(page + 1)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 transition hover:bg-slate-50 disabled:opacity-40"
          >
            下一页
          </button>
        </div>
      </div>

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold text-slate-800">{editingId ? '编辑用户' : '新增用户'}</h3>
            <div className="mt-4 space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-600">用户名</label>
                <input
                  value={form.username}
                  disabled={!!editingId}
                  onChange={(e) => setForm({ ...form, username: e.target.value })}
                  className={inputCls}
                  placeholder="用户名"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-600">
                  {editingId ? '密码（留空则不修改）' : '密码'}
                </label>
                <input
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  className={inputCls}
                  placeholder={editingId ? '留空则不修改密码' : '请输入密码'}
                />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-600">昵称</label>
                <input
                  value={form.nickname}
                  onChange={(e) => setForm({ ...form, nickname: e.target.value })}
                  className={inputCls}
                  placeholder="昵称"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-600">角色</label>
                <select
                  value={form.role}
                  onChange={(e) => setForm({ ...form, role: e.target.value })}
                  className={inputCls}
                >
                  <option value="USER">USER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </div>
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <button
                onClick={() => setModalOpen(false)}
                className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-50"
              >
                取消
              </button>
              <button
                onClick={submit}
                className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-700"
              >
                保存
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
