import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Clock, TrendingUp } from 'lucide-react';
import { EmptyState } from '../components/ui/EmptyState';
import { appGrowthService } from '../services/modules/appGrowth';
import type { GrowthOverview, GrowthLog } from '../types/growth';
import { useToast } from '../context/ToastContext';
import { cn } from '../lib/utils';
import { getGrowthTracePresentation } from '../utils/assetTracePresentation';

export default function GrowthCenter() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { tenantId: tenantIdParam } = useParams<{ tenantId: string }>();
  const tenantId = Number(tenantIdParam);

  const [overview, setOverview] = useState<GrowthOverview | null>(null);
  const [logs, setLogs] = useState<GrowthLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  const loadData = async (page = 1) => {
    if (!tenantId || isNaN(tenantId)) {
      showToast('缺少商户参数', 'error');
      setIsLoading(false);
      return;
    }
    try {
      if (page === 1) {
        const overviewData = await appGrowthService.getGrowthOverview(tenantId);
        setOverview(overviewData);
      }

      const logsData = await appGrowthService.getGrowthLogs(tenantId, page, 20);
      setLogs(logsData.records ?? []);
      setTotalPages(logsData.pages ?? 1);
      setCurrentPage(page);
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '获取成长值数据失败';
      showToast(message, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const handlePageChange = (page: number) => {
    if (page < 1 || page > totalPages || page === currentPage) return;
    void loadData(page);
  };

  const formatDate = (isoString: string) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return isoString.split('T')[0] || '';
    return date
      .toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      })
      .replace(/\//g, '-');
  };

  // Progress bar: current growth vs next-level threshold
  const progressPct =
    overview?.nextLevelGrowth && overview.nextLevelGrowth > 0
      ? Math.min(100, Math.round((overview.totalGrowth / overview.nextLevelGrowth) * 100))
      : 100;

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 pb-12 md:mt-8">
      {/* Header */}
      <header className="flex items-center justify-between border-b border-slate-100 pb-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="p-2 text-slate-600 hover:bg-slate-50 dark:hover:bg-slate-800 rounded-full transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-black text-slate-900 dark:text-white">成长中心</h1>
            <p className="text-xs font-semibold text-slate-400 mt-0.5">
              消费积累成长值，享受更多会员权益
            </p>
          </div>
        </div>
      </header>

      {/* Growth Overview Card */}
      <section className="relative overflow-hidden bg-slate-900 text-white rounded-3xl p-6 sm:p-8 shadow-xl shadow-slate-900/10">
        <div className="pointer-events-none absolute -right-20 -top-20 h-64 w-64 rounded-full bg-emerald-500/20 blur-3xl" />
        <div className="relative z-10 flex flex-col gap-5">
          <div className="flex items-center justify-between">
            <div>
              <span className="text-[10px] font-black uppercase tracking-widest text-slate-400 flex items-center gap-1.5">
                <TrendingUp className="w-4 h-4 text-emerald-400" />
                当前成长值
              </span>
              <div className="text-5xl font-black tracking-tight text-white mt-2 flex items-baseline gap-1">
                {isLoading ? '...' : (overview?.totalGrowth ?? 0).toLocaleString()}
                <span className="text-sm font-semibold text-slate-400 ml-1">点</span>
              </div>
            </div>
            <div className="flex flex-col items-end gap-1">
              <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">
                当前等级
              </span>
              <span className="text-2xl font-black text-emerald-400">
                {isLoading ? '...' : (overview?.levelName ?? '-')}
              </span>
            </div>
          </div>

          {/* Progress bar */}
          {!isLoading && overview && (
            <div className="flex flex-col gap-2">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-400">
                <span>{overview.levelName}</span>
                <span>
                  {overview.nextLevelGrowth != null
                    ? `${overview.totalGrowth} / ${overview.nextLevelGrowth}`
                    : '已达最高等级'}
                </span>
              </div>
              <div className="h-2.5 w-full rounded-full bg-white/10 overflow-hidden">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: `${progressPct}%` }}
                  transition={{ duration: 0.8, ease: 'easeOut' }}
                  className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-emerald-400"
                />
              </div>
              {overview.nextLevelGrowth != null && (
                <p className="text-[11px] text-slate-400 font-medium">
                  再积累 {(overview.nextLevelGrowth - overview.totalGrowth).toLocaleString()} 点成长值即可升级
                </p>
              )}
              {overview.nextLevelGrowth == null && (
                <p className="text-[11px] text-emerald-400 font-medium">
                  恭喜您已达最高等级，尽享尊享权益！
                </p>
              )}
            </div>
          )}
        </div>
      </section>

      {/* Growth Log Section */}
      <section>
        <h2 className="mb-3 ml-1 text-[11px] font-black uppercase tracking-[0.2em] text-slate-400">
          成长值明细
        </h2>
        <div className="min-h-[300px]">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3 text-slate-400">
              <div className="w-8 h-8 border-2 border-primary/20 border-t-primary rounded-full animate-spin" />
              <span className="text-sm font-medium">获取成长值数据中...</span>
            </div>
          ) : (
            <AnimatePresence mode="wait">
              <motion.div
                key={currentPage}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.15 }}
              >
                {logs.length === 0 ? (
                  <EmptyState
                    icon={<Clock className="w-12 h-12" />}
                    title="暂无成长值明细"
                    subtitle="您还没有成长值变动记录，快去消费积累吧！"
                  />
                ) : (
                  <div className="flex flex-col gap-3">
                    <div className="overflow-hidden rounded-3xl border border-slate-100 bg-white dark:bg-slate-900 shadow-sm divide-y divide-slate-50">
                      {logs.map((log) => {
                        const trace = getGrowthTracePresentation(log);
                        const isEarn = trace.tone === 'positive';
                        const isDeduct = trace.tone === 'negative';
                        return (
                          <div
                            key={log.id}
                            className="flex items-center justify-between p-5 transition-colors hover:bg-slate-50/50"
                          >
                            <div className="flex items-center gap-4">
                              <div
                                className={cn(
                                  'flex h-10 w-10 items-center justify-center rounded-2xl font-black text-sm',
                                  isEarn
                                    ? 'bg-emerald-50 text-emerald-600'
                                    : isDeduct
                                      ? 'bg-red-50 text-red-600'
                                      : 'bg-blue-50 text-blue-600'
                                )}
                              >
                                {isEarn ? '+' : isDeduct ? '-' : '~'}
                              </div>
                              <div>
                                <div className="font-extrabold text-slate-800 dark:text-white">
                                  {trace.title}
                                </div>
                                <div className="mt-0.5 flex flex-wrap items-center gap-1.5 text-xs font-semibold text-slate-400">
                                  <span>{trace.source}</span>
                                  <span className="text-slate-200">•</span>
                                  <span>{formatDate(log.createTime)}</span>
                                </div>
                                {trace.actionPath && (
                                  <button
                                    type="button"
                                    onClick={() => navigate(trace.actionPath!)}
                                    className="mt-1 text-[11px] font-bold text-primary hover:text-primary/80"
                                  >
                                    {trace.actionLabel}
                                  </button>
                                )}
                              </div>
                            </div>
                            <div className="text-right">
                              <div
                                className={cn(
                                  'text-lg font-black',
                                  isEarn
                                    ? 'text-emerald-600'
                                    : isDeduct
                                      ? 'text-red-600'
                                      : 'text-blue-600'
                                )}
                              >
                                {trace.effect}
                              </div>
                              <div className="text-[10px] font-bold text-slate-400 mt-0.5">
                                {trace.balance}
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </motion.div>
            </AnimatePresence>
          )}
        </div>

        {/* Pagination */}
        {!isLoading && totalPages > 1 && (
          <div className="mt-6 flex items-center justify-center gap-2">
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage <= 1}
              className={cn(
                'px-4 py-2 rounded-2xl text-sm font-bold transition-all',
                currentPage <= 1
                  ? 'text-slate-300 cursor-not-allowed'
                  : 'text-slate-600 hover:bg-slate-100'
              )}
            >
              上一页
            </button>
            <span className="text-sm font-semibold text-slate-400">
              {currentPage} / {totalPages}
            </span>
            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage >= totalPages}
              className={cn(
                'px-4 py-2 rounded-2xl text-sm font-bold transition-all',
                currentPage >= totalPages
                  ? 'text-slate-300 cursor-not-allowed'
                  : 'text-slate-600 hover:bg-slate-100'
              )}
            >
              下一页
            </button>
          </div>
        )}
      </section>
    </div>
  );
}
