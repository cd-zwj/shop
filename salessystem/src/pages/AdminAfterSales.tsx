import { useCallback, useEffect, useRef, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  FileSearch,
  HeartHandshake,
  RefreshCw,
  Search,
  ShieldCheck,
  X,
  XCircle,
} from 'lucide-react';
import { Pagination } from '../components/Pagination';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { adminAfterSaleService } from '../services/modules/adminAfterSale';
import type { AdminAfterSale, AfterSaleAction } from '../types/refund';
import { hasAdminPermission } from '../utils/adminPermissions';
import { formatCurrencyFen } from '../utils/display';
import { getErrorMessage } from '../utils/errorMessage';

const PAGE_SIZE = 20;
const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'PROCESSING', label: '退款中' },
  { value: 'COMPLETED', label: '已退款' },
  { value: 'FAILED', label: '退款失败' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'CANCELLED', label: '已取消' },
];

interface Filters {
  status: string;
  tenantId?: number;
  keyword: string;
}

const EMPTY_FILTERS: Filters = { status: '', keyword: '' };

export default function AdminAfterSales() {
  const { showToast } = useToast();
  const { adminSession } = useAuth();
  const canManage = hasAdminPermission(adminSession?.permissions, 'admin:after-sale:manage');
  const [refunds, setRefunds] = useState<AdminAfterSale[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);
  const [statusInput, setStatusInput] = useState('');
  const [tenantInput, setTenantInput] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [selected, setSelected] = useState<AdminAfterSale | null>(null);
  const [actions, setActions] = useState<AfterSaleAction[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [approved, setApproved] = useState(true);
  const [remark, setRemark] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);
  const listRequestRef = useRef(0);
  const detailRequestRef = useRef(0);

  const loadRefunds = useCallback(async () => {
    const requestId = ++listRequestRef.current;
    setLoading(true);
    setLoadError('');
    try {
      const result = await adminAfterSaleService.listRefunds({
        status: filters.status || undefined,
        tenantId: filters.tenantId,
        keyword: filters.keyword || undefined,
        pageNum: page,
        pageSize: PAGE_SIZE,
      });
      if (requestId !== listRequestRef.current) return;
      setRefunds([...(result.records ?? [])]);
      setTotal(result.total ?? 0);
    } catch (error) {
      if (requestId !== listRequestRef.current) return;
      setRefunds([]);
      setTotal(0);
      setLoadError(getErrorMessage(error, '平台售后列表加载失败，请稍后重试'));
    } finally {
      if (requestId === listRequestRef.current) setLoading(false);
    }
  }, [filters, page]);

  useEffect(() => {
    void loadRefunds();
  }, [loadRefunds]);

  const applyFilters = (event: React.FormEvent) => {
    event.preventDefault();
    const parsedTenantId = tenantInput ? Number(tenantInput) : undefined;
    if (parsedTenantId !== undefined && (!Number.isInteger(parsedTenantId) || parsedTenantId <= 0)) {
      showToast('租户 ID 必须是大于 0 的整数', 'error');
      return;
    }
    setPage(1);
    setFilters({
      status: statusInput,
      tenantId: parsedTenantId,
      keyword: keywordInput.trim(),
    });
  };

  const resetFilters = () => {
    setStatusInput('');
    setTenantInput('');
    setKeywordInput('');
    setPage(1);
    setFilters({ ...EMPTY_FILTERS });
  };

  const openDetail = async (refund: AdminAfterSale) => {
    const requestId = ++detailRequestRef.current;
    setSelected(refund);
    setActions([]);
    setApproved(true);
    setRemark('');
    setDetailLoading(true);
    setDetailError('');
    try {
      const [detail, actionList] = await Promise.all([
        adminAfterSaleService.getRefund(refund.tenantId, refund.id),
        adminAfterSaleService.listActions(refund.tenantId, refund.id),
      ]);
      if (requestId !== detailRequestRef.current) return;
      setSelected({ ...detail });
      setActions([...(actionList ?? [])]);
    } catch (error) {
      if (requestId !== detailRequestRef.current) return;
      setDetailError(getErrorMessage(error, '售后详情加载失败，请稍后重试'));
    } finally {
      if (requestId === detailRequestRef.current) setDetailLoading(false);
    }
  };

  const closeDetail = () => {
    if (submitting) return;
    detailRequestRef.current += 1;
    setSelected(null);
  };

  const submitDecision = async () => {
    if (!selected || submittingRef.current) return;
    const normalizedRemark = remark.trim();
    if (!normalizedRemark) {
      showToast('请填写平台处理说明', 'error');
      return;
    }
    if (normalizedRemark.length > 1000) {
      showToast('平台处理说明不能超过 1000 个字符', 'error');
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    try {
      await adminAfterSaleService.intervene(
        selected.tenantId,
        selected.id,
        selected.refundStatus,
        approved,
        normalizedRemark,
      );
      showToast('平台售后决定已提交', 'success');
      setSelected(null);
      setRemark('');
      await loadRefunds();
    } catch (error) {
      showToast(getErrorMessage(error, '平台售后处理失败，请稍后重试'), 'error');
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-6 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-[1440px] space-y-5">
        <header className="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 pb-5">
          <div>
            <div className="mb-1 flex items-center gap-2 text-sm font-semibold text-primary">
              <HeartHandshake className="h-4 w-4" /> 平台运营
            </div>
            <h1 className="text-2xl font-black text-slate-900">售后运营工作台</h1>
          </div>
          <div className="text-right">
            <p className="text-xs font-semibold text-slate-500">当前结果</p>
            <p className="text-xl font-black text-slate-900">{total} <span className="text-xs font-semibold text-slate-400">笔售后</span></p>
          </div>
        </header>

        <form onSubmit={applyFilters} className="grid gap-3 border-b border-slate-200 pb-5 sm:grid-cols-2 xl:grid-cols-[180px_180px_minmax(240px,1fr)_auto]">
          <label className="grid gap-1.5 text-xs font-bold text-slate-600">
            状态
            <select
              value={statusInput}
              onChange={(event) => setStatusInput(event.target.value)}
              className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-800 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
            >
              {STATUS_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
          </label>
          <label className="grid gap-1.5 text-xs font-bold text-slate-600">
            租户 ID
            <input
              aria-label="租户 ID"
              inputMode="numeric"
              value={tenantInput}
              onChange={(event) => setTenantInput(event.target.value)}
              className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
              placeholder="全部租户"
            />
          </label>
          <label className="grid gap-1.5 text-xs font-bold text-slate-600">
            单号搜索
            <input
              aria-label="退款单号或订单号"
              maxLength={64}
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
              className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
              placeholder="退款单号或订单号"
            />
          </label>
          <div className="flex items-end gap-2">
            <button type="submit" className="inline-flex h-10 items-center gap-2 rounded-md bg-slate-900 px-4 text-sm font-bold text-white hover:bg-slate-800">
              <Search className="h-4 w-4" /> 查询
            </button>
            <button type="button" onClick={resetFilters} className="h-10 rounded-md border border-slate-300 bg-white px-4 text-sm font-bold text-slate-600 hover:bg-slate-100">
              重置
            </button>
          </div>
        </form>

        {loading ? (
          <LoadingState label="正在加载售后队列" />
        ) : loadError ? (
          <ErrorState title="售后队列加载失败" message={loadError} onRetry={() => void loadRefunds()} />
        ) : refunds.length === 0 ? (
          <div className="flex min-h-[360px] flex-col items-center justify-center gap-3 border border-dashed border-slate-300 bg-white text-slate-500">
            <FileSearch className="h-10 w-10 text-slate-300" />
            <p className="text-sm font-bold">没有符合条件的售后单</p>
          </div>
        ) : (
          <div className="overflow-hidden border border-slate-200 bg-white">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[920px] border-collapse text-left">
                <thead className="bg-slate-50 text-xs font-bold text-slate-500">
                  <tr>
                    <th className="px-4 py-3">退款单 / 订单</th>
                    <th className="px-4 py-3">租户</th>
                    <th className="px-4 py-3">原因</th>
                    <th className="px-4 py-3">金额</th>
                    <th className="px-4 py-3">状态</th>
                    <th className="px-4 py-3">更新时间</th>
                    <th className="px-4 py-3 text-right">操作</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {refunds.map((refund) => (
                    <tr key={`${refund.tenantId}-${refund.id}`} className="text-sm text-slate-700 hover:bg-slate-50/70">
                      <td className="px-4 py-4">
                        <p className="font-bold text-slate-900">{refund.refundNo}</p>
                        <p className="mt-1 text-xs text-slate-500">{refund.orderNo}</p>
                      </td>
                      <td className="px-4 py-4 font-semibold">租户 {refund.tenantId}</td>
                      <td className="max-w-[260px] px-4 py-4"><p className="truncate" title={refund.reason}>{refund.reason}</p></td>
                      <td className="px-4 py-4 font-bold text-slate-900">{formatCurrencyFen(refund.refundAmount)}</td>
                      <td className="px-4 py-4"><StatusBadge refund={refund} /></td>
                      <td className="px-4 py-4 text-xs text-slate-500">{refund.updateTime || refund.createTime}</td>
                      <td className="px-4 py-4 text-right">
                        <button type="button" onClick={() => void openDetail(refund)} className="rounded-md border border-slate-300 px-3 py-2 text-xs font-bold text-slate-700 hover:border-primary hover:text-primary">
                          查看详情
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination current={page} total={total} pageSize={PAGE_SIZE} onChange={setPage} className="border-t border-slate-200 px-4 py-3" />
          </div>
        )}
      </div>

      {selected && (
        <DetailDrawer
          refund={selected}
          actions={actions}
          loading={detailLoading}
          error={detailError}
          approved={approved}
          remark={remark}
          submitting={submitting}
          canManage={canManage}
          onApprovedChange={setApproved}
          onRemarkChange={setRemark}
          onClose={closeDetail}
          onRetry={() => void openDetail(selected)}
          onSubmit={() => void submitDecision()}
        />
      )}
    </main>
  );
}

function DetailDrawer({
  refund,
  actions,
  loading,
  error,
  approved,
  remark,
  submitting,
  canManage,
  onApprovedChange,
  onRemarkChange,
  onClose,
  onRetry,
  onSubmit,
}: {
  refund: AdminAfterSale;
  actions: AfterSaleAction[];
  loading: boolean;
  error: string;
  approved: boolean;
  remark: string;
  submitting: boolean;
  canManage: boolean;
  onApprovedChange: (value: boolean) => void;
  onRemarkChange: (value: string) => void;
  onClose: () => void;
  onRetry: () => void;
  onSubmit: () => void;
}) {
  const canIntervene = refund.refundStatus === 'PENDING' || refund.refundStatus === 'REJECTED';

  return (
    <div className="fixed inset-0 z-[80] flex justify-end bg-slate-950/45" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <aside role="dialog" aria-modal="true" aria-label="售后详情" className="h-full w-full max-w-2xl overflow-y-auto bg-white shadow-2xl">
        <header className="sticky top-0 z-10 flex items-start justify-between gap-4 border-b border-slate-200 bg-white px-5 py-4">
          <div>
            <p className="text-xs font-bold text-slate-500">租户 {refund.tenantId}</p>
            <h2 className="mt-1 text-lg font-black text-slate-900">{refund.refundNo}</h2>
          </div>
          <button type="button" aria-label="关闭售后详情" disabled={submitting} onClick={onClose} className="rounded-md p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-50">
            <X className="h-5 w-5" />
          </button>
        </header>

        {loading ? <LoadingState label="正在加载售后详情" /> : error ? (
          <ErrorState title="详情加载失败" message={error} onRetry={onRetry} />
        ) : (
          <div className="space-y-7 p-5">
            <section aria-labelledby="refund-summary-title">
              <div className="mb-3 flex items-center justify-between gap-3">
                <h3 id="refund-summary-title" className="text-sm font-black text-slate-900">售后摘要</h3>
                <StatusBadge refund={refund} />
              </div>
              <dl className="grid grid-cols-2 gap-x-5 gap-y-4 border-y border-slate-200 py-4 text-sm">
                <DetailTerm label="订单号" value={refund.orderNo} />
                <DetailTerm label="退款金额" value={formatCurrencyFen(refund.refundAmount)} />
                <DetailTerm label="申请原因" value={refund.reason} />
                <DetailTerm label="交付状态" value={refund.deliveryStatus || '未记录'} />
                <DetailTerm label="申请说明" value={refund.description || '未填写'} wide />
                <DetailTerm label="处理建议" value={refund.refundSuggestion || '无'} wide />
              </dl>
              {(refund.evidenceUrls?.length ?? 0) > 0 && (
                <div className="mt-4">
                  <p className="mb-2 text-xs font-bold text-slate-500">申请凭证</p>
                  <div className="flex flex-wrap gap-2">
                    {(refund.evidenceUrls ?? []).map((url, index) => evidenceHost(url) ? (
                      <a key={`${url}-${index}`} href={url} target="_blank" rel="noreferrer" className="rounded-md border border-slate-300 px-3 py-2 text-xs font-bold text-primary hover:bg-primary/5">
                        查看凭证 {index + 1} · {evidenceHost(url)}
                      </a>
                    ) : null)}
                  </div>
                </div>
              )}
            </section>

            <section aria-labelledby="action-timeline-title">
              <h3 id="action-timeline-title" className="mb-4 text-sm font-black text-slate-900">处理时间线</h3>
              {actions.length === 0 ? <p className="text-sm text-slate-500">暂无处理记录</p> : (
                <ol className="space-y-4 border-l border-slate-200 pl-5">
                  {actions.map((action, index) => (
                    <li key={`${action.action}-${action.createTime ?? index}`} className="relative">
                      <span className="absolute -left-[25px] top-1.5 h-2 w-2 rounded-full bg-primary ring-4 ring-white" />
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <p className="text-sm font-bold text-slate-900">{actionLabel(action.action)} · {roleLabel(action.operatorRole)}</p>
                        <time className="text-xs text-slate-400">{action.createTime || '时间未记录'}</time>
                      </div>
                      {action.remark && <p className="mt-1.5 whitespace-pre-wrap text-sm leading-6 text-slate-600">{action.remark}</p>}
                    </li>
                  ))}
                </ol>
              )}
            </section>

            <section aria-labelledby="intervention-title" className="border-t border-slate-200 pt-6">
              <div className="mb-4 flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary" />
                <h3 id="intervention-title" className="text-sm font-black text-slate-900">平台处理</h3>
              </div>
              {!canManage ? (
                <p className="rounded-md bg-slate-100 px-3 py-3 text-sm font-semibold text-slate-600">当前账号没有平台售后处理权限</p>
              ) : !canIntervene ? (
                <p className="rounded-md bg-slate-100 px-3 py-3 text-sm font-semibold text-slate-600">当前状态无需平台介入</p>
              ) : (
                <div className="space-y-4">
                  {refund.refundStatus === 'REJECTED' && (
                    <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs font-semibold text-amber-800">
                      已驳回售后仅可由平台重新同意退款
                    </p>
                  )}
                  <div className={`grid rounded-md border border-slate-300 p-1 ${refund.refundStatus === 'PENDING' ? 'grid-cols-2' : 'grid-cols-1'}`} aria-label="平台处理决定">
                    <DecisionButton active={approved} onClick={() => onApprovedChange(true)} icon={<CheckCircle2 className="h-4 w-4" />} label="同意退款" />
                    {refund.refundStatus === 'PENDING' && (
                      <DecisionButton active={!approved} onClick={() => onApprovedChange(false)} icon={<XCircle className="h-4 w-4" />} label="驳回申请" />
                    )}
                  </div>
                  <label className="grid gap-2 text-xs font-bold text-slate-600">
                    平台处理说明 <span className="font-medium text-slate-400">{remark.length}/1000</span>
                    <textarea
                      aria-label="平台处理说明"
                      rows={5}
                      maxLength={1100}
                      value={remark}
                      onChange={(event) => onRemarkChange(event.target.value)}
                      disabled={submitting}
                      className="resize-y rounded-md border border-slate-300 p-3 text-sm font-normal text-slate-800 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15 disabled:bg-slate-100"
                      placeholder="填写核验结论和处理依据"
                    />
                  </label>
                  <button type="button" onClick={onSubmit} disabled={submitting} className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-md bg-slate-900 px-4 text-sm font-bold text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60">
                    {submitting ? <RefreshCw className="h-4 w-4 animate-spin" /> : approved ? <CheckCircle2 className="h-4 w-4" /> : <XCircle className="h-4 w-4" />}
                    {submitting ? '正在提交' : approved ? '确认同意退款' : '确认驳回申请'}
                  </button>
                </div>
              )}
            </section>
          </div>
        )}
      </aside>
    </div>
  );
}

function DecisionButton({ active, onClick, icon, label }: { active: boolean; onClick: () => void; icon: React.ReactNode; label: string }) {
  return (
    <button type="button" aria-pressed={active} onClick={onClick} className={`inline-flex h-9 items-center justify-center gap-2 rounded px-3 text-sm font-bold ${active ? 'bg-slate-900 text-white' : 'text-slate-500 hover:bg-slate-100'}`}>
      {icon}{label}
    </button>
  );
}

function DetailTerm({ label, value, wide = false }: { label: string; value: string; wide?: boolean }) {
  return <div className={wide ? 'col-span-2' : ''}><dt className="text-xs font-bold text-slate-400">{label}</dt><dd className="mt-1 break-words font-semibold text-slate-800">{value}</dd></div>;
}

function StatusBadge({ refund }: { refund: AdminAfterSale }) {
  const status = refund.refundStatus;
  const tone = status === 'COMPLETED' || status === 'APPROVED'
    ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
    : status === 'FAILED' || status === 'REJECTED'
      ? 'border-red-200 bg-red-50 text-red-700'
      : status === 'PENDING'
        ? 'border-amber-200 bg-amber-50 text-amber-700'
        : 'border-slate-200 bg-slate-100 text-slate-600';
  return <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-bold ${tone}`}>{refund.statusLabel || statusLabel(status)}</span>;
}

function LoadingState({ label }: { label: string }) {
  return <div className="flex min-h-[360px] flex-col items-center justify-center gap-3 text-sm font-semibold text-slate-500"><RefreshCw className="h-7 w-7 animate-spin text-primary" />{label}</div>;
}

function ErrorState({ title, message, onRetry }: { title: string; message: string; onRetry: () => void }) {
  return (
    <div role="alert" className="flex min-h-[360px] flex-col items-center justify-center gap-3 border border-red-200 bg-red-50 p-5 text-center">
      <AlertCircle className="h-8 w-8 text-red-500" />
      <div><h2 className="text-sm font-black text-slate-900">{title}</h2><p className="mt-1 text-sm text-red-700">{message}</p></div>
      <button type="button" onClick={onRetry} className="inline-flex items-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-bold text-white"><RefreshCw className="h-4 w-4" />重试</button>
    </div>
  );
}

function statusLabel(status: string) {
  return STATUS_OPTIONS.find((option) => option.value === status)?.label || status;
}

function actionLabel(action: string) {
  const labels: Record<string, string> = {
    USER_APPLY: '提交申请',
    USER_CANCEL: '用户取消',
    MERCHANT_APPROVE: '商户同意',
    MERCHANT_REJECT: '商户驳回',
    PLATFORM_APPROVE: '平台同意',
    PLATFORM_REJECT: '平台驳回',
    PLATFORM_APPROVE_FAILED: '平台处理失败',
    PLATFORM_REJECT_FAILED: '平台处理失败',
    REFUND_PROCESSING: '退款处理中',
    REFUND_COMPLETED: '退款完成',
    REFUND_FAILED: '退款失败',
  };
  return labels[action] || action;
}

function roleLabel(role: string) {
  const labels: Record<string, string> = { USER: '用户', MERCHANT: '商户', ADMIN: '平台', SYSTEM: '系统' };
  return labels[role] || role;
}

function evidenceHost(url: string) {
  try {
    const parsed = new URL(url);
    return parsed.protocol === 'https:' || parsed.protocol === 'http:' ? parsed.hostname : '';
  } catch {
    return '';
  }
}
