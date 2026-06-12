import { cn } from '../lib/utils';

interface PaginationProps {
  current: number;
  total: number;
  pageSize: number;
  onChange: (page: number) => void;
  className?: string;
}

/**
 * 通用分页组件。
 *
 * @example
 * <Pagination current={1} total={50} pageSize={10} onChange={setPage} />
 */
export function Pagination({ current, total, pageSize, onChange, className }: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className={cn('flex items-center justify-between', className)}>
      <span className="text-xs font-medium text-slate-400">
        共 {total} 条
      </span>
      <div className="flex items-center gap-2">
        <button
          disabled={current <= 1}
          onClick={() => onChange(current - 1)}
          className="rounded-xl px-3 py-1.5 text-xs font-black bg-slate-100 text-slate-500 hover:bg-slate-200 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
        >
          上一页
        </button>
        <span className="text-xs font-bold text-slate-600 min-w-[4rem] text-center">
          {current} / {totalPages}
        </span>
        <button
          disabled={current >= totalPages}
          onClick={() => onChange(current + 1)}
          className="rounded-xl px-3 py-1.5 text-xs font-black bg-slate-100 text-slate-500 hover:bg-slate-200 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
        >
          下一页
        </button>
      </div>
    </div>
  );
}
