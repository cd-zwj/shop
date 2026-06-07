import { type ReactNode } from 'react';
import { cn } from '../../lib/utils';

interface EmptyStateProps {
  icon: ReactNode;
  title: string;
  subtitle: string;
  className?: string;
}

export function EmptyState({ icon, title, subtitle, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center py-20 px-4 text-center rounded-3xl border border-slate-100 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm',
        className,
      )}
    >
      <div className="p-4 rounded-full bg-slate-50 dark:bg-slate-800/50 text-slate-400 mb-4">
        {icon}
      </div>
      <h3 className="text-base font-extrabold text-slate-800 dark:text-white">{title}</h3>
      <p className="text-xs font-semibold text-slate-400 mt-1.5 max-w-xs leading-relaxed">{subtitle}</p>
    </div>
  );
}
