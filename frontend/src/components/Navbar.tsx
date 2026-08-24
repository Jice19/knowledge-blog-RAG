import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { listCategories, type Category } from '../api/category'
import { logout as apiLogout } from '../api/auth'

interface StoredUser {
  userId: number
  username: string
  nickname?: string
  role?: string
}

export default function Navbar() {
  const [categories, setCategories] = useState<Category[]>([])
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    listCategories()
      .then(setCategories)
      .catch(() => {})
  }, [])

  const token = localStorage.getItem('akb_token')
  let user: StoredUser | null = null
  try {
    user = JSON.parse(localStorage.getItem('akb_user') || 'null')
  } catch {
    user = null
  }

  const linkCls = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-medium transition-colors ${
      isActive ? 'text-indigo-600' : 'text-slate-600 hover:text-slate-900'
    }`

  const logout = async () => {
    try {
      await apiLogout()
    } catch {
      // 忽略
    }
    localStorage.removeItem('akb_token')
    localStorage.removeItem('akb_user')
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/80 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4 sm:px-6">
        <Link to="/" className="flex shrink-0 items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 text-sm font-bold text-white">
            AI
          </span>
          <span className="text-lg font-bold tracking-tight text-slate-900">智识博客</span>
        </Link>

        <nav className="hidden items-center gap-5 md:flex">
          <NavLink to="/" end className={linkCls}>
            首页
          </NavLink>
          <NavLink to="/rag" className={linkCls}>
            AI 问答
          </NavLink>
          {categories.slice(0, 4).map((c) => (
            <NavLink key={c.id} to={`/category/${c.slug}`} className={linkCls}>
              {c.name}
            </NavLink>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-3">
          {token && user ? (
            user.role === 'ADMIN' ? (
              <div className="flex items-center gap-2">
                <Link
                  to="/admin"
                  className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700"
                >
                  管理后台
                </Link>
                <button onClick={logout} className="text-sm text-slate-500 transition hover:text-slate-700">
                  退出
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <span className="text-sm text-slate-600">{user.nickname || user.username}</span>
                <button onClick={logout} className="text-sm text-slate-500 transition hover:text-slate-700">
                  退出
                </button>
              </div>
            )
          ) : (
            <Link
              to="/login"
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700"
            >
              登录
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
