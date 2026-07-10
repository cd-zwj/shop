import { useCallback, useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, ArrowLeft, Clock, RefreshCw, TrendingUp } from 'lucide-react';
import { EmptyState } from '../components/ui/EmptyState';
import { appGrowthService } from '../services/modules/appGrowth';
import type { GrowthOverview, GrowthLog } from '../types/growth';
import { useToast } from '../context/ToastContext';
import { cn } from '../lib/utils';
import { getGrowthTracePresentation } from '../utils/assetTracePresentation';
import { getErrorMessage } from '../utils/errorMessage';
import { getPageTotalPages } from '../utils/pageResult';

export default function GrowthCenter() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { tenantId: tenantIdParam } = useParams<{ tenantId: string }>();
  const tenantId = Number(tenantIdParam);

  const [overview, setOverview] = useState<GrowthOverview | null>(null);
  const [logs, setLogs] = useState<GrowthLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  const loadData = useCallback(async (page = 1) => {
    if (!tenantId || isNaN(tenantId)) {
      const message = '缺少商户参数';
      setError(message);
      showToast(message, 'error');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      if (page === 1) {
        const overviewData = await appGrowthService.getGrowthOverview(tenantId);
        setOverview(overviewData);
      }

      const logsData = await appGrowthService.getGrowthLogs(tenantId, page, 20);
      setLogs(logsData.records ?? []);
      setTotalPages(Math.max(1, getPageTotalPages(logsData)));
      setCurrentPage(page);
    } catch (e: unknown) {
      const message = getErrorMessage(e, '获取成长值数据失败');
      setError(message);
      setOverview(null);
      setLogs([]);
      setTotalPages(1);
      setCurrentPage(1);
      showToast(message, 'error');
    } finally {
      setIsLoading(false);
    }
  }, [showToast, tenantId]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

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
  const benefitItems = parseBenefitItems(overview?.benefitJson);

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

      {error && (
        <div className="flex flex-col gap-4 rounded-3xl border border-red-100 bg-red-50 px-6 py-5 text-red-700 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <AlertCircle className="h-5 w-5 flex-none" />
            <span className="text-sm font-bold">{error}</span>
          </div>
          <button
            type="button"
            onClick={() => void loadData()}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-white px-4 py-2 text-sm font-black text-red-700 shadow-sm transition-all hover:bg-red-100"
          >
            <RefreshCw className="h-4 w-4" />
            重试
          </button>
        </div>
      )}

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
              {(overview.discountRate || benefitItems.length > 0) && (
                <div className="mt-3 rounded-2xl bg-white/10 p-4">
                  <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">当前权益</p>
                  <div className="mt-2 flex flex-wrap gap-2">
                    {overview.discountRate && overview.discountRate > 0 && overview.discountRate < 1 && (
                      <span className="rounded-xl bg-emerald-400/15 px-3 py-1 text-xs font-black text-emerald-300">
                        {(overview.discountRate * 10).toFixed(1).replace(/\.0$/, '')} 折
                      </span>
                    )}
                    {benefitItems.map((item) => (
                      <span key={item} className="rounded-xl bg-white/10 px-3 py-1 text-xs font-bold text-white">
                        {item}
                      </span>
                    ))}
                  </div>
                </div>
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

function parseBenefitItems(value?: string | null) {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) {
      return parsed.map(String).filter(Boolean).slice(0, 6);
    }
    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed as Record<string, unknown>)
        .map(([key, entry]) => `${key}: ${String(entry)}`)
        .slice(0, 6);
    }
  } catch {
    return value.split(/[,\n]/).map((item) => item.trim()).filter(Boolean).slice(0, 6);
  }
  return [];
}
