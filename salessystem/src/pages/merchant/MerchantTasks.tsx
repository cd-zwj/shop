import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertCircle, ArrowRight, ClipboardList, RefreshCw, ServerCog } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { merchantWorkbenchService } from '../../services/modules/merchantWorkbench';
import type { MerchantWorkbenchTask, MerchantWorkbenchTaskSource } from '../../types/merchant';
import { cn } from '../../lib/utils';
import { getErrorMessage } from '../../utils/errorMessage';

const TASK_TABS: Array<{ id: MerchantWorkbenchTaskSource; label: string; description: string }> = [
  { id: 'compensation', label: '补偿任务', description: '订单、支付、退款补偿任务的商户可见跟进列表。' },
  { id: 'retry', label: '重试任务', description: '消息或异步任务重试失败后的商户可见跟进列表。' },
];

function normalizeTaskType(value: string | null): MerchantWorkbenchTaskSource {
  return value === 'retry' ? 'retry' : 'compensation';
}

export default function MerchantTasks() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const [tasks, setTasks] = useState<MerchantWorkbenchTask[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const activeType = normalizeTaskType(searchParams.get('type'));
  const activeTab = TASK_TABS.find((tab) => tab.id === activeType) ?? TASK_TABS[0];
  const pageSize = 20;

  const loadTasks = useCallback(async () => {
    if (!tenantId) {
      setError('当前商户会话缺少 tenantId，请重新登录');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    try {
      const result = await merchantWorkbenchService.listTasks(tenantId, {
        type: activeType,
        pageNum: currentPage,
        pageSize,
      });
      setTasks(result.records ?? []);
      setTotal(result.total ?? 0);
      setError('');
    } catch (loadError) {
      setTasks([]);
      setTotal(0);
      setError(getErrorMessage(loadError, '系统任务加载失败，请稍后重试'));
    } finally {
      setIsLoading(false);
    }
  }, [activeType, currentPage, tenantId]);

  useEffect(() => {
    void loadTasks();
  }, [loadTasks, reloadKey]);

  const totalPages = useMemo(() => Math.max(1, Math.ceil(total / pageSize)), [total]);

  function handleTabChange(type: MerchantWorkbenchTaskSource) {
    setCurrentPage(1);
    setSearchParams({ type });
  }

  function handleTaskAction(task: MerchantWorkbenchTask) {
    if (task.actionPath) {
      navigate(task.actionPath);
    }
  }

  return (
    <div className="flex flex-col gap-6 p-4 pb-24 md:p-8">
      <header className="flex flex-col gap-2">
        <h1 className="text-3xl font-black tracking-tight text-slate-900">系统任务跟进</h1>
        <p className="text-sm font-medium leading-relaxed text-slate-500">
          {activeTab.description} 商家可查看原因并回到业务单跟进，任务重试和取消由管理员处理。
        </p>
      </header>

      <div className="flex gap-2 overflow-x-auto border-b border-slate-100">
        {TASK_TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => handleTabChange(tab.id)}
            className={cn(
              'whitespace-nowrap border-b-2 px-5 pb-4 text-sm font-black transition-all',
              activeType === tab.id
                ? 'border-primary text-primary'
                : 'border-transparent text-slate-400 hover:text-slate-700',
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error && (
        <div role="alert" className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
            <button
              type="button"
              onClick={() => setReloadKey((key) => key + 1)}
              className="inline-flex w-fit items-center gap-2 rounded-xl bg-white px-4 py-2 text-xs font-black text-red-700 shadow-sm hover:bg-red-100"
            >
              <RefreshCw className="h-4 w-4" />
              重试
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <SummaryCard label="任务类型" value={activeTab.label} />
        <SummaryCard label="当前总数" value={isLoading ? '...' : String(total)} />
        <SummaryCard label="当前页" value={`${currentPage} / ${totalPages}`} />
      </div>

      <section className="overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-xl shadow-slate-100/40">
        <div className="flex items-center justify-between border-b border-slate-50 px-6 py-5">
          <div>
            <h2 className="text-sm font-black text-slate-900">任务列表</h2>
            <p className="mt-1 text-xs font-medium text-slate-400">只展示与当前商户相关且未闭环的系统任务。</p>
          </div>
          <button
            type="button"
            onClick={() => setReloadKey((key) => key + 1)}
            className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-2 text-xs font-black text-slate-600 hover:border-primary hover:text-primary"
          >
            <RefreshCw className="h-4 w-4" />
            刷新
          </button>
        </div>

        {isLoading ? (
          <div className="space-y-3 p-6">
            {Array.from({ length: 4 }).map((_, index) => (
              <div key={index} className="h-24 animate-pulse rounded-2xl bg-slate-50" />
            ))}
          </div>
        ) : tasks.length === 0 ? (
          <div className="flex min-h-[280px] flex-col items-center justify-center gap-3 px-4 py-12 text-center text-slate-400">
            <ClipboardList className="h-12 w-12 text-slate-300" />
            <p className="text-sm font-black text-slate-500">暂无系统任务</p>
            <p className="max-w-sm text-xs font-medium leading-relaxed">
              当前类型下没有需要商家跟进的未闭环任务。
            </p>
          </div>
        ) : (
          <div className="divide-y divide-slate-50">
            {tasks.map((task) => (
              <article key={`${task.taskSource}-${task.id}`} className="grid gap-4 px-6 py-5 lg:grid-cols-[1fr_auto]">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className={cn('rounded-lg px-2.5 py-1 text-[10px] font-black uppercase tracking-widest', getStatusClass(task.taskStatus))}>
                      {task.taskStatus}
                    </span>
                    <span className="rounded-lg bg-slate-100 px-2.5 py-1 text-[10px] font-black uppercase tracking-widest text-slate-500">
                      {task.taskSource}
                    </span>
                  </div>
                  <h3 className="mt-3 break-all font-mono text-sm font-black text-slate-900">
                    {task.taskNo}
                  </h3>
                  <p className="mt-1 break-all text-xs font-semibold text-slate-500">
                    {task.bizType || task.taskType || '--'} / {task.bizNo || '--'}
                  </p>
                  <div className="mt-3 grid gap-2 text-xs font-medium text-slate-500 sm:grid-cols-2">
                    <p>重试次数：{task.retryCount ?? 0}{task.maxRetryCount ? ` / ${task.maxRetryCount}` : ''}</p>
                    <p>下次重试：{formatTaskTime(task.nextRetryTime)}</p>
                    <p>创建时间：{formatTaskTime(task.createTime)}</p>
                    <p>更新时间：{formatTaskTime(task.updateTime)}</p>
                  </div>
                  {task.lastError && (
                    <div className="mt-4 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-xs font-semibold leading-relaxed text-red-700">
                      {task.lastError}
                    </div>
                  )}
                </div>
                <div className="flex items-center lg:justify-end">
                  <button
                    type="button"
                    onClick={() => handleTaskAction(task)}
                    disabled={!task.actionPath}
                    className="inline-flex items-center gap-2 rounded-2xl bg-slate-900 px-5 py-3 text-xs font-black text-white transition-all hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {task.actionLabel || '查看业务'}
                    <ArrowRight className="h-4 w-4" />
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button
            type="button"
            onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
            disabled={currentPage === 1}
            className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            上一页
          </button>
          <button
            type="button"
            onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
            disabled={currentPage === totalPages}
            className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            下一页
          </button>
        </div>
      )}
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[28px] border border-slate-100 bg-white p-5 shadow-sm">
      <div className="mb-4 w-fit rounded-2xl bg-primary/5 p-3 text-primary">
        <ServerCog className="h-5 w-5" />
      </div>
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className="mt-2 text-2xl font-black tracking-tight text-slate-900">{value}</p>
    </div>
  );
}

function getStatusClass(status: string) {
  if (status === 'FAIL' || status === 'DEAD') {
    return 'bg-red-50 text-red-700';
  }
  if (status === 'PROCESSING') {
    return 'bg-blue-50 text-blue-700';
  }
  return 'bg-orange-50 text-orange-700';
}

function formatTaskTime(value?: string | null) {
  if (!value) {
    return '--';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}
