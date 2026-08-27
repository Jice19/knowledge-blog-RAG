import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { logout as apiLogout } from '../api/auth'

const navItems = [
  { to: '/admin', label: '仪表盘', icon: '📊', end: true },
  { to: '/admin/users', label: '用户管理', icon: '👤' },
  { to: '/admin/articles', label: '文章管理', icon: '📝' },
  { to: '/admin/categories', label: '分类管理', icon: '🗂️' },
  { to: '/admin/tags', label: '标签管理', icon: '🏷️' },
  { to: '/admin/notes', label: '笔记导入', icon: '📥' },
]

interface StoredUser {
  userId: number
  username: string
  nickname?: string
  role?: string
}

export default function AdminLayout() {
  const navigate = useNavigate()
  const [user, setUser] = useState<StoredUser | null>(() => {
    try {
      return JSON.parse(localStorage.getItem('akb_user') || 'null')
    } catch {
      return null
    }
  })

  useEffect(() => {
    const token = localStorage.getItem('akb_token')
    if (!token) {
      navigate('/login', { replace: true })
      return
    }
    // 非管理员不能访问后台
    if (user && user.role && user.role !== 'ADMIN') {
      navigate('/', { replace: true })
    }
  }, [navigate, user])

  const logout = async () => {
    try {
      await apiLogout()
    } catch {
      // token 可能已过期，忽略
    }
    localStorage.removeItem('akb_token')
    localStorage.removeItem('akb_user')
    navigate('/login')
  }

  const displayName = user?.nickname || user?.username || 'J'

  return (
    <div className="flex min-h-screen bg-slate-100">
      {/* 侧边栏 */}
      <aside className="hidden w-60 flex-col border-r border-slate-200 bg-white md:flex">
        <div className="flex h-16 items-center gap-2 border-b border-slate-100 px-5">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 text-sm font-bold text-white">
            AI
          </span>
          <span className="font-bold text-slate-900">博客管理</span>
        </div>
        <nav className="flex-1 space-y-1 p-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                  isActive ? 'bg-indigo-50 text-indigo-600' : 'text-slate-600 hover:bg-slate-100'
                }`
              }
            >
              <span>{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-100 p-3">
          <Link
            to="/"
            className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm text-slate-500 transition hover:bg-slate-100"
          >
            ↩ 返回前台
          </Link>
        </div>
      </aside>

      {/* 主区域 */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-6">
          <h1 className="text-base font-semibold text-slate-800">内容管理</h1>
          <div className="flex items-center gap-3">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-100 text-sm font-bold text-indigo-600">
              {displayName[0]}
            </span>
            <span className="text-sm text-slate-600">{displayName}</span>
            <button
              onClick={logout}
              className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm text-slate-600 transition hover:bg-slate-50"
            >
              退出
            </button>
          </div>
        </header>
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
