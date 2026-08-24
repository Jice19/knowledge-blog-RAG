import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { pageArticles, type ArticleVO } from '../../api/article'
import { listCategories, type Category } from '../../api/category'
import ArticleCard from '../../components/ArticleCard'

export default function CategoryPage() {
  const { slug } = useParams()
  const [category, setCategory] = useState<Category | null>(null)
  const [articles, setArticles] = useState<ArticleVO[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    listCategories()
      .then((cats) => {
        const c = cats.find((x) => x.slug === slug) ?? null
        setCategory(c)
        if (c) {
          return pageArticles({ page: 1, size: 100, categoryId: c.id })
        }
        return null
      })
      .then((data) => setArticles(data?.records ?? []))
      .catch(() => setArticles([]))
      .finally(() => setLoading(false))
  }, [slug])

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <h1 className="text-2xl font-bold text-slate-900">{category?.name ?? '分类'}</h1>
      <p className="mt-1 text-sm text-slate-500">共 {articles.length} 篇文章</p>

      {loading ? (
        <div className="py-24 text-center text-slate-400">加载中…</div>
      ) : (
        <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {articles.map((a) => (
            <ArticleCard key={a.id} article={a} />
          ))}
        </div>
      )}

      {!loading && articles.length === 0 && (
        <div className="py-24 text-center text-slate-400">该分类下暂无文章</div>
      )}
    </div>
  )
}
