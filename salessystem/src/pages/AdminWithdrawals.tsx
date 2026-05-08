import { useEffect, useMemo, useState } from 'react';
import { Search } from 'lucide-react';
import { adminWithdrawalService } from '../services/modules/adminWithdrawal';
import { ApiError } from '../types/api';
import type { AdminWithdrawal } from '../types/admin';
import { formatCurrency } from '../utils/display';

export default function AdminWithdrawals() {
  const [merchantName, setMerchantName] = useState('');
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  const [rejectingId, setRejectingId] = useState<number | null>(null);
  const [approvingId, setApprovingId] = useState<number | null>(null);
  const [withdrawals, setWithdrawals] = useState<AdminWithdrawal[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadWithdrawals() {
      try {
        const result = await adminWithdrawalService.listWithdrawals({
          current: 1,
          size: 50,
          merchantName: merchantName.trim() || undefined,
          status: statusFilter,
        });
        if (!isMounted) return;
        setWithdrawals(result.records ?? []);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('提现审核列表加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadWithdrawals();

    return () => {
      isMounted = false;
    };
  }, [merchantName, statusFilter]);

  const pendingCount = useMemo(
    () => withdrawals.filter((item) => item.status === 0).length,
    [withdrawals],
  );

  const totalAmount = useMemo(
    () => withdrawals.reduce((sum, item) => sum + Number(item.amount || 0), 0),
    [withdrawals],
  );

  async function refreshList() {
    const result = await adminWithdrawalService.listWithdrawals({
      current: 1,
      size: 50,
      merchantName: merchantName.trim() || undefined,
      status: statusFilter,
    });
    setWithdrawals(result.records ?? []);
  }

  async function approve(item: AdminWithdrawal) {
    setApprovingId(item.id);
    setError('');
    setSuccess('');
    try {
      await adminWithdrawalService.approveWithdrawal(item.id);
      await refreshList();
      setSuccess(`提现单 ${item.id} 已审核通过`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '提现审核通过失败');
    } finally {
      setApprovingId(null);
    }
  }

  async function reject(item: AdminWithdrawal) {
    const reason = window.prompt('请输入驳回原因', item.rejectReason || '');
    if (reason === null) return;

    setRejectingId(item.id);
    setError('');
    setSuccess('');
    try {
      await adminWithdrawalService.rejectWithdrawal(item.id, reason.trim());
      await refreshList();
      setSuccess(`提现单 ${item.id} 已驳回`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '提现驳回失败');
    } finally {
      setRejectingId(null);
    }
  }

  const rows: Array<AdminWithdrawal | null> = isLoading ? Array.from({ length: 5 }, () => null) : withdrawals;

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header>
        <h1 className="text-3xl font-black tracking-tight text-slate-900">提现审核中心</h1>
        <p className="mt-1 text-sm font-medium text-slate-500">
          当前页接入 `/v1/admin/withdrawals`、`approve` 和 `reject`，可直接完成提现审核闭环。
        </p>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-green-100 bg-green-50 px-4 py-3 text-sm font-medium text-green-600">
          {success}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard label="当前列表提现单" value={String(withdrawals.length)} />
        <SummaryCard label="待审核提现单" value={String(pendingCount)} />
        <SummaryCard label="当前列表提现总额" value={formatCurrency(totalAmount)} />
      </div>

      <div className="rounded-[24px] border border-slate-100 bg-white p-4 shadow-sm">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="relative">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={merchantName}
              onChange={(event) => setMerchantName(event.target.value)}
              placeholder="按商户名称筛选"
              className="w-full rounded-2xl border border-slate-100 bg-slate-50 py-3 pl-11 pr-4 text-sm font-medium outline-none focus:border-primary focus:bg-white"
            />
          </div>
          <select
            value={statusFilter ?? ''}
            onChange={(event) =>
              setStatusFilter(event.target.value === '' ? undefined : Number(event.target.value))
            }
            className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm font-black text-slate-700 outline-none focus:border-primary focus:bg-white"
          >
            <option value="">全部状态</option>
            <option value="0">待审核</option>
            <option value="1">已通过</option>
            <option value="2">已驳回</option>
          </select>
        </div>
      </div>

      <div className="overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-xl shadow-slate-100/40">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-slate-50/50">
              <tr>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">提现单号</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">商户与账户</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">金额</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">状态</th>
                <th className="px-8 py-5"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {rows.map((item, index) => (
                <tr
                  key={item ? item.id : `loading-${index}`}
                  className="transition-colors hover:bg-slate-50/50"
                >
                  <td className="px-8 py-6">
                    <p className="text-sm font-black text-slate-900">{item ? item.id : '加载中...'}</p>
                    <p className="mt-1 text-xs font-medium text-slate-400">
                      {item ? formatDateTime(item.applyTime || item.createTime) : '--'}
                    </p>
                  </td>
                  <td className="px-8 py-6">
                    <p className="text-sm font-black text-slate-900">{item?.merchantName || '--'}</p>
                    <p className="mt-1 text-xs font-medium text-slate-400">
                      {item ? `${item.bankName || '--'} / ${maskBankAccount(item.bankAccount)}` : '--'}
                    </p>
                    <p className="mt-1 text-xs font-medium text-slate-400">{item?.accountName || '--'}</p>
                  </td>
                  <td className="px-8 py-6">
                    <p className="text-sm font-black text-slate-900">
                      {item ? formatCurrency(item.amount) : '--'}
                    </p>
                    {item?.approverName && (
                      <p className="mt-1 text-xs font-medium text-slate-400">审核人：{item.approverName}</p>
                    )}
                  </td>
                  <td className="px-8 py-6">
                    <span className={getStatusClassName(item?.status ?? 0)}>
                      {item ? getStatusText(item.status) : '--'}
                    </span>
                    {item?.status === 2 && item.rejectReason && (
                      <p className="mt-2 text-xs font-medium text-red-500">{item.rejectReason}</p>
                    )}
                  </td>
                  <td className="px-8 py-6 text-right">
                    {item?.status === 0 && (
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => void approve(item)}
                          disabled={approvingId === item.id}
                          className="rounded-xl bg-primary px-4 py-2 text-xs font-black text-white shadow-lg shadow-primary/20 transition-all hover:scale-105 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          通过
                        </button>
                        <button
                          onClick={() => void reject(item)}
                          disabled={rejectingId === item.id}
                          className="rounded-xl border border-red-200 px-4 py-2 text-xs font-black text-red-500 transition-all hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          驳回
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className="mt-2 text-2xl font-black tracking-tight text-slate-900">{value}</p>
    </div>
  );
}

function getStatusText(status: number) {
  if (status === 1) return '已通过';
  if (status === 2) return '已驳回';
  return '待审核';
}

function getStatusClassName(status: number) {
  if (status === 1) return 'rounded-lg bg-green-100 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-green-700';
  if (status === 2) return 'rounded-lg bg-red-100 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-red-700';
  return 'rounded-lg bg-orange-100 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-orange-700';
}

function maskBankAccount(bankAccount?: string | null) {
  if (!bankAccount) return '--';
  const clean = bankAccount.replace(/\s+/g, '');
  return clean.length <= 4 ? clean : `****${clean.slice(-4)}`;
}

function formatDateTime(value?: string | null) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}
