import { Navigate, Route, Routes } from 'react-router-dom'
import BlogLayout from './layouts/BlogLayout'
import AdminLayout from './components/AdminLayout'
import HomePage from './pages/blog/HomePage'
import ArticlePage from './pages/blog/ArticlePage'
import CategoryPage from './pages/blog/CategoryPage'
import LoginPage from './pages/admin/LoginPage'
import DashboardPage from './pages/admin/DashboardPage'
import UserManagePage from './pages/admin/UserManagePage'
import ArticleListPage from './pages/admin/ArticleListPage'
import ArticleEditPage from './pages/admin/ArticleEditPage'
import CategoryManagePage from './pages/admin/CategoryManagePage'
import TagManagePage from './pages/admin/TagManagePage'

export default function App() {
  return (
    <Routes>
      {/* 前台 */}
      <Route element={<BlogLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/article/:id" element={<ArticlePage />} />
        <Route path="/category/:slug" element={<CategoryPage />} />
      </Route>

      {/* 后台 */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="users" element={<UserManagePage />} />
        <Route path="articles" element={<ArticleListPage />} />
        <Route path="articles/new" element={<ArticleEditPage />} />
        <Route path="articles/:id/edit" element={<ArticleEditPage />} />
        <Route path="categories" element={<CategoryManagePage />} />
        <Route path="tags" element={<TagManagePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
