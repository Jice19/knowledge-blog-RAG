import { Outlet } from 'react-router-dom'
import Navbar from '../components/Navbar'

export default function BlogLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 bg-white py-8">
        <div className="mx-auto max-w-6xl px-4 text-center text-sm text-slate-400">
          © 2026 AI 智识博客 · 前端转全栈学习项目
        </div>
      </footer>
    </div>
  )
}
