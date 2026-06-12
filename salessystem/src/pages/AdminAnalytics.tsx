import { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import {
  Zap,
  Database,
  ChevronRight,
  Brain,
  Globe,
  Cpu,
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  Tooltip,
  ResponsiveContainer,
  LineChart as ReLineChart,
  Line,
} from 'recharts';
import { cn } from '../lib/utils';
import { adminDashboardService } from '../services/modules/adminDashboard';
import type { AdminDashboardOverview, AdminTrendPoint } from '../types/admin';

/* ---------- Types ---------- */

interface TrendChartPoint {
  date: string;
  orderAmount: number;
  orderCount: number;
}

/* ---------- Sub-components ---------- */

function MetricSkeleton() {
  return (
    <div className="bg-white border border-slate-100 p-6 rounded-3xl shadow-sm flex items-center gap-6 animate-pulse">
      <div className="w-14 h-14 rounded-2xl bg-slate-100" />
      <div className="flex-1 space-y-2">
        <div className="h-3 w-20 bg-slate-100 rounded" />
        <div className="h-6 w-16 bg-slate-200 rounded" />
      </div>
    </div>
  );
}

function ChartSkeleton({ dark }: { dark?: boolean }) {
  return (
    <div
      className={cn(
        'rounded-[40px] p-10 animate-pulse flex flex-col gap-6',
        dark ? 'bg-slate-900' : 'bg-white border border-slate-100',
      )}
    >
      <div className={cn('h-5 w-48 rounded', dark ? 'bg-white/10' : 'bg-slate-100')} />
      <div className={cn('h-[320px] rounded-2xl', dark ? 'bg-white/5' : 'bg-slate-50')} />
    </div>
  );
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center justify-center h-full gap-2 text-slate-400">
      <Database className="w-8 h-8" />
      <span className="text-sm font-medium">{message}</span>
    </div>
  );
}

/* ---------- Formatting helpers ---------- */

function formatCount(n: number | undefined | null): string {
  if (n == null) return '--';
  if (n >= 10000) return `${(n / 10000).toFixed(1)} 万`;
  return n.toLocaleString('zh-CN');
}

function formatCurrency(n: number | undefined | null): string {
  if (n == null) return '--';
  if (n >= 10000) return `${(n / 10000).toFixed(1)} 万`;
  return `¥${n.toLocaleString('zh-CN')}`;
}

/** 将 ISO 日期 (yyyy-MM-dd) 转为短显示 (MM/dd) */
function toShortDate(isoDate: string): string {
  const parts = isoDate.split('-');
  if (parts.length === 3) {
    return `${parts[1]}/${parts[2]}`;
  }
  return isoDate;
}

/* ---------- Main Component ---------- */

export default function AdminAnalytics() {
  const [overview, setOverview] = useState<AdminDashboardOverview | null>(null);
  const [trendData, setTrendData] = useState<TrendChartPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      try {
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(endDate.getDate() - 29);

        const fmt = (d: Date) =>
          `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

        const [overviewRes, trendRes] = await Promise.allSettled([
          adminDashboardService.getOverview(),
          adminDashboardService.getTrend({
            startDate: fmt(startDate),
            endDate: fmt(endDate),
            granularity: 'DAY',
          }),
        ]);

        if (cancelled) return;

        if (overviewRes.status === 'fulfilled') {
          setOverview(overviewRes.value);
        }

        if (trendRes.status === 'fulfilled' && trendRes.value.points?.length > 0) {
          setTrendData(
            trendRes.value.points.map((p: AdminTrendPoint) => ({
              date: toShortDate(p.date),
              orderAmount: p.orderAmount ?? 0,
              orderCount: p.orderCount ?? 0,
            })),
          );
        } else {
          setTrendData([]);
        }

        if (overviewRes.status === 'rejected') {
          setError('无法加载仪表盘数据，请稍后重试');
        }
      } catch (err: unknown) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '数据加载失败');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  /* ---------- Derived display values ---------- */
  const hasRealTrendData = trendData.some((d) => d.orderAmount > 0 || d.orderCount > 0);
  const metrics = overview
    ? [
        { label: '平台用户总数', value: formatCount(overview.totalPlatformUsers), icon: Cpu, color: 'text-primary' },
        { label: '签约商户数', value: formatCount(overview.totalMerchants), icon: Database, color: 'text-tertiary' },
        { label: '累计订单数', value: formatCount(overview.totalOrders), icon: Zap, color: 'text-orange-500' },
        { label: '已支付订单率', value: overview.totalOrders > 0 ? `${((overview.paidOrders / overview.totalOrders) * 100).toFixed(1)}%` : '0%', icon: Brain, color: 'text-indigo-500' },
      ]
    : [
        { label: '平台用户总数', value: '--', icon: Cpu, color: 'text-primary' },
        { label: '签约商户数', value: '--', icon: Database, color: 'text-tertiary' },
        { label: '累计订单数', value: '--', icon: Zap, color: 'text-orange-500' },
        { label: '已支付订单率', value: '--', icon: Brain, color: 'text-indigo-500' },
      ];

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-black text-slate-900 tracking-tight">智能引擎与深度分析</h1>
          <p className="text-slate-500 font-medium font-inter">基于平台实时数据的全局流量分析与趋势监控。</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="px-4 py-2 bg-primary/10 text-primary border border-primary/20 rounded-xl flex items-center gap-2">
            <div className="w-2 h-2 bg-primary rounded-full animate-pulse" />
            <span className="text-[10px] font-black uppercase tracking-widest font-inter">数据监听中</span>
          </div>
        </div>
      </header>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-2xl p-4 text-red-700 text-sm font-medium">
          {error}
        </div>
      )}

      {/* Hero Analytics Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {loading ? (
          <div className="lg:col-span-2">
            <ChartSkeleton dark />
          </div>
        ) : (
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="lg:col-span-2 bg-slate-900 rounded-[40px] p-10 text-white shadow-2xl relative overflow-hidden"
          >
            {/* Background visuals */}
            <div className="absolute inset-0 opacity-10">
              <div className="absolute left-0 top-0 w-full h-full border-[1px] border-white/20 translate-x-12 translate-y-12 rounded-full" />
              <div className="absolute left-0 top-0 w-full h-full border-[1px] border-white/20 translate-x-24 translate-y-24 rounded-full" />
            </div>

            <div className="relative z-10">
              <div className="flex items-center gap-4 mb-8">
                <div className="w-14 h-14 bg-white/10 backdrop-blur-xl rounded-2xl flex items-center justify-center border border-white/20 shadow-xl">
                  <Globe className="w-8 h-8 text-primary" />
                </div>
                <div>
                  <h3 className="text-2xl font-black tracking-tight">订单金额增长趋势</h3>
                  <p className="text-slate-400 font-medium text-sm font-inter">
                    近 30 日每日订单金额走势（真实数据）
                  </p>
                </div>
              </div>

              <div className="h-[320px] w-full">
                {hasRealTrendData ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <ReLineChart data={trendData}>
                      <defs>
                        <filter id="shadow" height="200%">
                          <feGaussianBlur in="SourceAlpha" stdDeviation="3" />
                          <feOffset dx="0" dy="4" result="offsetblur" />
                          <feComponentTransfer>
                            <feFuncA type="linear" slope="0.5" />
                          </feComponentTransfer>
                          <feMerge>
                            <feMergeNode />
                            <feMergeNode in="SourceGraphic" />
                          </feMerge>
                        </filter>
                      </defs>
                      <XAxis
                        dataKey="date"
                        axisLine={false}
                        tickLine={false}
                        tick={{ fontSize: 10, fontWeight: 700, fill: '#64748b' }}
                        interval="preserveStartEnd"
                      />
                      <Tooltip
                        contentStyle={{ backgroundColor: '#0f172a', border: '1px solid #1e293b', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold' }}
                        itemStyle={{ color: '#fff' }}
                        formatter={(value: number, name: string) => [
                          name === 'orderAmount' ? formatCurrency(value) : value,
                          name === 'orderAmount' ? '订单金额' : '订单数',
                        ]}
                      />
                      <Line
                        type="monotone"
                        dataKey="orderAmount"
                        stroke="#0ea5e9"
                        strokeWidth={4}
                        dot={false}
                        activeDot={{ r: 8, stroke: '#fff', strokeWidth: 4 }}
                        filter="url(#shadow)"
                      />
                    </ReLineChart>
                  </ResponsiveContainer>
                ) : (
                  <EmptyState message="暂无趋势数据" />
                )}
              </div>

              <div className="flex gap-10 mt-10 border-t border-white/5 pt-8">
                <div>
                  <div className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-1">累计订单金额</div>
                  <div className="text-3xl font-black text-white tracking-tight">
                    {overview ? formatCurrency(overview.totalOrderAmount) : '--'}
                  </div>
                </div>
                <div>
                  <div className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-1">累计支付金额</div>
                  <div className="text-3xl font-black text-white tracking-tight">
                    {overview ? formatCurrency(overview.totalPaymentAmount) : '--'}
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {loading ? (
          <ChartSkeleton />
        ) : (
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="bg-white rounded-[40px] border border-slate-100 p-10 shadow-xl shadow-slate-200/40 flex flex-col gap-10"
          >
            <div className="flex flex-col gap-3">
              <div className="w-12 h-12 bg-orange-50 text-orange-500 rounded-2xl flex items-center justify-center border border-orange-100">
                <Zap className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-black text-slate-900">每日订单量</h3>
              <p className="text-sm text-slate-500 leading-relaxed font-inter font-medium">近 30 日每日订单数分布。</p>
            </div>

            <div className="h-[200px] w-full">
              {hasRealTrendData ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={trendData}>
                    <Bar dataKey="orderCount" fill="#cbd5e1" radius={[8, 8, 0, 0]} />
                    <XAxis
                      dataKey="date"
                      axisLine={false}
                      tickLine={false}
                      tick={{ fontSize: 10, fontWeight: 700, fill: '#94a3b8' }}
                      interval="preserveStartEnd"
                    />
                    <Tooltip
                      contentStyle={{ backgroundColor: '#0f172a', border: '1px solid #1e293b', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold' }}
                      itemStyle={{ color: '#fff' }}
                      formatter={(value: number) => [value, '订单数']}
                    />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <EmptyState message="暂无订单数据" />
              )}
            </div>

            <div className="flex flex-col gap-4">
              <div className="p-4 bg-red-50 rounded-2xl flex items-center justify-between group cursor-pointer hover:bg-red-100 transition-colors">
                <div className="flex items-center gap-3">
                  <Brain className="w-5 h-5 text-red-500" />
                  <span className="text-sm font-black text-red-700">
                    {overview?.pendingWithdrawals
                      ? `待处理提现: ${overview.pendingWithdrawals} 笔`
                      : '暂无待处理提现'}
                  </span>
                </div>
                <ChevronRight className="w-4 h-4 text-red-400 group-hover:translate-x-1 transition-transform" />
              </div>
              <button className="w-full py-4 border-2 border-slate-100 text-slate-500 font-black text-sm rounded-2xl hover:bg-slate-50 transition-all">
                配置检测规则
              </button>
            </div>
          </motion.div>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 mb-10">
        {loading
          ? Array.from({ length: 4 }).map((_, i) => <MetricSkeleton key={i} />)
          : metrics.map((item, i) => (
              <motion.div
                key={i}
                whileHover={{ y: -4, shadow: '0 20px 25px -5px rgba(0, 0, 0, 0.05)' }}
                className="bg-white border border-slate-100 p-6 rounded-3xl shadow-sm flex items-center gap-6 group cursor-pointer"
              >
                <div className={cn('w-14 h-14 rounded-2xl bg-slate-50 flex items-center justify-center transition-all group-hover:scale-110 shadow-inner', item.color)}>
                  <item.icon className="w-7 h-7" />
                </div>
                <div>
                  <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{item.label}</p>
                  <p className="text-2xl font-black text-slate-900 mt-1">{item.value}</p>
                </div>
              </motion.div>
            ))}
      </div>
    </div>
  );
}
