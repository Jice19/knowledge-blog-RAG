import { useParams } from 'react-router-dom'
import { useData } from '../../store/DataContext'
import ArticleCard from '../../components/ArticleCard'

export default function CategoryPage() {
  const { slug } = useParams()
  const { articles, categories } = useData()
  const category = categories.find((c) => c.slug === slug)
  const list = articles.filter((a) => a.status === 1 && a.category?.slug === slug)

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <h1 className="text-2xl font-bold text-slate-900">{category?.name ?? '分类'}</h1>
      <p className="mt-1 text-sm text-slate-500">共 {list.length} 篇文章</p>

      <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {list.map((a) => (
          <ArticleCard key={a.id} article={a} />
        ))}
      </div>

      {list.length === 0 && (
        <div className="py-24 text-center text-slate-400">该分类下暂无文章</div>
      )}
    </div>
  )
}
