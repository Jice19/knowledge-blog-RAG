export default function StatusBadge({ status }: { status: number }) {
  return status === 1 ? (
    <span className="inline-flex items-center rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-600">
      <span className="mr-1 h-1.5 w-1.5 rounded-full bg-emerald-500" />
      已发布
    </span>
  ) : (
    <span className="inline-flex items-center rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-medium text-amber-600">
      <span className="mr-1 h-1.5 w-1.5 rounded-full bg-amber-500" />
      草稿
    </span>
  )
}
