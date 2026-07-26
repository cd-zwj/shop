import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, ArrowLeft, Clock, Coins, RefreshCw, Sparkles } from 'lucide-react';
import { EmptyState } from '../components/ui/EmptyState';
import { useToast } from '../context/ToastContext';
import { appPointsService } from '../services/modules/appPoints';
import type { PointsBalance, PointsLog } from '../types/points';
import { getPointsTracePresentation } from '../utils/assetTracePresentation';
import { getErrorMessage } from '../utils/errorMessage';
import { cn } from '../lib/utils';

export default function Points() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { tenantId: tenantIdParam } = useParams<{ tenantId: string }>();
  const tenantId = Number(tenantIdParam);
  const [balance, setBalance] = useState<PointsBalance | null>(null);
  const [logs, setLogs] = useState<PointsLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadPointsData = useCallback(async () => {
    if (!tenantId || Number.isNaN(tenantId)) {
      const message = '缺少商户参数';
      setError(message);
      showToast(message, 'error');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const [balanceData, logsData] = await Promise.all([
        appPointsService.getPointsBalance(tenantId),
        appPointsService.getPointsLogs(tenantId, 1, 20),
      ]);
      setBalance(balanceData);
      setLogs(logsData.records ?? []);
    } catch (err) {
      const message = getErrorMessage(err, '获取积分中心数据失败');
      setError(message);
      setBalance(null);
      setLogs([]);
      showToast(message, 'error');
    } finally {
      setIsLoading(false);
    }
  }, [showToast, tenantId]);

  useEffect(() => {
    void loadPointsData();
  }, [loadPointsData]);

  const expiringSoonPoints = balance?.expiringSoonPoints ?? 0;
  const groups = groupLogsByDate(logs);

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 pb-12 md:mt-8">
      <header className="flex items-center gap-3 border-b border-slate-100 pb-4">
        <button type="button" onClick={() => navigate(-1)} className="rounded-full p-2 text-slate-600 transition-colors hover:bg-slate-50">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-2xl font-black text-slate-900">积分中心</h1>
          <p className="mt-0.5 text-xs font-semibold text-slate-400">消费可获得积分，积分仅用于门店经营活动。</p>
        </div>
      </header>

      {error && (
        <div className="flex flex-col gap-4 rounded-lg border border-red-100 bg-red-50 px-6 py-5 text-red-700 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3"><AlertCircle className="h-5 w-5 flex-none" /><span className="text-sm font-bold">{error}</span></div>
          <button type="button" onClick={() => void loadPointsData()} className="inline-flex items-center justify-center gap-2 rounded-md bg-white px-4 py-2 text-sm font-bold text-red-700 shadow-sm hover:bg-red-100">
            <RefreshCw className="h-4 w-4" />重试
          </button>
        </div>
      )}

      <section className="bg-slate-900 p-6 text-white shadow-xl shadow-slate-900/10 sm:p-8">
        <span className="flex items-center gap-1.5 text-[10px] font-black uppercase text-slate-400"><Coins className="h-4 w-4 fill-yellow-500 text-yellow-500" />当前可用积分</span>
        <div className="mt-2 flex items-baseline gap-1 text-5xl font-black">{isLoading ? '...' : (balance?.points ?? 0).toLocaleString()}<span className="ml-1 text-sm font-semibold text-slate-400">分</span></div>
        <p className="mt-3 flex items-center gap-1 text-xs font-semibold text-slate-400"><Sparkles className="h-3.5 w-3.5 text-yellow-500" />积分余额和明细仅反映已生效的门店活动。</p>
        {expiringSoonPoints > 0 && <p className="mt-3 inline-flex items-center gap-1.5 bg-yellow-500/15 px-3 py-1 text-xs font-bold text-yellow-200"><Clock className="h-3.5 w-3.5" />近 30 天将过期 {expiringSoonPoints.toLocaleString()} 分</p>}
      </section>

      <section className="min-h-[300px]">
        <h2 className="mb-4 text-lg font-black text-slate-900">积分明细</h2>
        {isLoading ? (
          <div className="flex flex-col items-center justify-center gap-3 py-20 text-slate-400"><div className="h-8 w-8 animate-spin rounded-full border-2 border-primary/20 border-t-primary" /><span className="text-sm font-medium">获取积分中心数据中...</span></div>
        ) : groups.length === 0 ? (
          <EmptyState icon={<Clock className="h-12 w-12" />} title="暂无积分明细" subtitle="您最近还没有积分变动。" />
        ) : (
          <div className="flex flex-col gap-6">
            {groups.map(([date, items]) => <div key={date} className="flex flex-col gap-3">
              <h3 className="ml-1 text-xs font-black uppercase text-slate-400">{date}</h3>
              <div className="divide-y divide-slate-50 border border-slate-100 bg-white shadow-sm">
                {items.map((log) => <PointsLogRow key={log.id} log={log} navigate={navigate} />)}
              </div>
            </div>)}
          </div>
        )}
      </section>
    </div>
  );
}

function PointsLogRow({ log, navigate }: { log: PointsLog; navigate: ReturnType<typeof useNavigate> }) {
  const trace = getPointsTracePresentation(log);
  const isGrant = trace.tone === 'positive';
  return <div className="flex items-center justify-between p-5 transition-colors hover:bg-slate-50/50">
    <div className="flex items-center gap-4">
      <div className={cn('flex h-10 w-10 items-center justify-center rounded-md font-black text-sm', isGrant ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-600')}>{isGrant ? '+' : '-'}</div>
      <div><div className="font-bold text-slate-800">{trace.title}</div><div className="mt-0.5 text-xs font-semibold text-slate-400">{trace.source} · {formatDate(log.createTime)}</div>{trace.actionPath && <button type="button" onClick={() => navigate(trace.actionPath!)} className="mt-1 text-[11px] font-bold text-primary">{trace.actionLabel}</button>}</div>
    </div>
    <div className={cn('text-lg font-black', isGrant ? 'text-green-600' : 'text-red-600')}>{trace.effect}</div>
  </div>;
}

function groupLogsByDate(logs: PointsLog[]) {
  const groups = new Map<string, PointsLog[]>();
  logs.forEach((log) => {
    const date = log.createTime?.split('T')[0] || '其他';
    groups.set(date, [...(groups.get(date) ?? []), log]);
  });
  return [...groups.entries()].sort(([left], [right]) => right.localeCompare(left));
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value?.split('T')[0] || '' : date.toLocaleString('zh-CN', { hour12: false });
}
