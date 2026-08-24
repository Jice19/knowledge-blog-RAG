/** 按分类返回封面的渐变（未上传封面时用作占位） */
export const categoryGradients: Record<string, string> = {
  backend: 'from-indigo-500 via-blue-500 to-indigo-600',
  frontend: 'from-sky-500 via-cyan-500 to-blue-500',
  database: 'from-emerald-500 via-teal-500 to-green-500',
  middleware: 'from-orange-500 via-amber-500 to-orange-600',
  ai: 'from-fuchsia-500 via-purple-500 to-violet-600',
  architecture: 'from-rose-500 via-pink-500 to-rose-600',
}

export function coverGradient(slug?: string): string {
  return categoryGradients[slug ?? ''] ?? 'from-slate-500 via-slate-400 to-slate-600'
}
