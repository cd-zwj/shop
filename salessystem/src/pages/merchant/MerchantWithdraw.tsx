import { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  ArrowUpRight,
  CheckCircle2,
  Clock,
  Landmark,
  Wallet,
  X,
  XCircle,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { merchantFinanceService } from '../../services/modules/merchantFinance';
import { ApiError } from '../../types/api';
import type {
  MerchantWalletSummary,
  MerchantWithdrawal,
  MerchantWithdrawalApplyPayload,
} from '../../types/merchant';
import { cn } from '../../lib/utils';
import { formatCurrency } from '../../utils/display';

const DEFAULT_SUMMARY: MerchantWalletSummary = {
  tenantId: 0,
  availableBalance: 0,
  frozenBalance: 0,
  totalIncome: 0,
  totalWithdrawal: 0,
  totalPlatformFee: 0,
};

const EMPTY_FORM: MerchantWithdrawalApplyPayload = {
  amount: 0,
  bankName: '',
  bankAccount: '',
  accountName: '',
};

export default function MerchantWithdraw() {
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const [summary, setSummary] = useState<MerchantWalletSummary>(DEFAULT_SUMMARY);
  const [withdrawals, setWithdrawals] = useState<MerchantWithdrawal[]>([]);
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState<MerchantWithdrawalApplyPayload>(EMPTY_FORM);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadWithdrawals() {
      if (!tenantId) {
        setError('当前商户会话缺少 tenantId，请重新登录');
        setIsLoading(false);
        return;
      }

      try {
        const [balance, withdrawalPage] = await Promise.all([
          merchantFinanceService.getWithdrawalBalance(tenantId),
          merchantFinanceService.listWithdrawals(tenantId, {
            current: 1,
            size: 20,
            status: statusFilter,
          }),
        ]);

        if (!isMounted) return;
        setSummary(balance);
        setWithdrawals(withdrawalPage.records ?? []);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('提现数据加载失败，请稍后重试');
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
  }, [statusFilter, tenantId]);

  const pendingCount = useMemo(
    () => withdrawals.filter((item) => item.status === 0).length,
    [withdrawals],
  );

  async function refreshData(nextStatusFilter = statusFilter) {
    if (!tenantId) {
      return;
    }

    const [balance, withdrawalPage] = await Promise.all([
      merchantFinanceService.getWithdrawalBalance(tenantId),
      merchantFinanceService.listWithdrawals(tenantId, {
        current: 1,
        size: 20,
        status: nextStatusFilter,
      }),
    ]);
    setSummary(balance);
    setWithdrawals(withdrawalPage.records ?? []);
  }

  function updateField<K extends keyof MerchantWithdrawalApplyPayload>(
    key: K,
    value: MerchantWithdrawalApplyPayload[K],
  ) {
    setFormData((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSubmit() {
    if (!tenantId) {
      setError('当前商户会话缺少 tenantId，请重新登录');
      return;
    }

    if (!Number.isFinite(Number(formData.amount)) || Number(formData.amount) <= 0) {
      setError('提现金额必须大于 0');
      return;
    }
    if (Number(formData.amount) > Number(summary.availableBalance || 0)) {
      setError('提现金额不能超过当前可提现余额');
      return;
    }
    if (!formData.bankName.trim() || !formData.bankAccount.trim() || !formData.accountName.trim()) {
      setError('请完整填写开户银行、银行卡号和账户名称');
      return;
    }

    setIsSubmitting(true);
    setError('');
    setSuccess('');

    try {
      await merchantFinanceService.createWithdrawal(tenantId, {
        amount: Number(formData.amount),
        bankName: formData.bankName.trim(),
        bankAccount: formData.bankAccount.trim(),
        accountName: formData.accountName.trim(),
      });
      await refreshData();
      setSuccess('提现申请已提交，等待管理员审核');
      setFormData(EMPTY_FORM);
      setShowForm(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '提现申请提交失败，请稍后重试');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 p-4 pb-32 md:p-8">
      <header className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end">
        <div>
          <h1 className="text-4xl font-black tracking-tight text-slate-900">商户提现中心</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            提现余额、历史记录和申请提交流程都已切到真实接口。
          </p>
        </div>
        <button
          onClick={() => setShowForm(true)}
          className="flex items-center justify-center gap-2 rounded-[24px] bg-primary px-8 py-4 text-base font-black text-white shadow-2xl shadow-primary/20 transition-all hover:scale-105 active:scale-95"
        >
          <ArrowUpRight className="h-5 w-5" /> 发起提现申请
        </button>
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
        {[
          {
            label: '可提现余额',
            value: formatCurrency(summary.availableBalance),
            hint: '当前可申请提现金额',
            icon: Wallet,
            tone: 'bg-slate-900 text-white',
          },
          {
            label: '冻结余额',
            value: formatCurrency(summary.frozenBalance),
            hint: '审核中或待结算资金',
            icon: Clock,
            tone: 'bg-white',
          },
          {
            label: '累计已提现',
            value: formatCurrency(summary.totalWithdrawal),
            hint: `${pendingCount} 笔待审核`,
            icon: Landmark,
            tone: 'bg-white',
          },
        ].map((card) => (
          <div
            key={card.label}
            className={cn(
              'rounded-[32px] border border-slate-100 p-6 shadow-xl shadow-slate-200/30',
              card.tone,
            )}
          >
            <div className="mb-5 flex items-start justify-between">
              <div className="rounded-2xl bg-primary/5 p-3 text-primary">
                <card.icon className="h-6 w-6" />
              </div>
              {isLoading && (
                <span className="rounded-xl bg-slate-100 px-2 py-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  loading
                </span>
              )}
            </div>
            <p className={cn('text-[10px] font-black uppercase tracking-widest', card.tone.includes('text-white') ? 'text-slate-500' : 'text-slate-400')}>
              {card.label}
            </p>
            <p className="mt-2 text-2xl font-black tracking-tight">{isLoading ? '...' : card.value}</p>
            <p
              className={cn(
                'mt-2 text-sm font-medium',
                card.tone.includes('text-white') ? 'text-slate-400' : 'text-slate-500',
              )}
            >
              {card.hint}
            </p>
          </div>
        ))}
      </div>

      <section className="rounded-[40px] border border-slate-100 bg-white shadow-sm">
        <div className="flex flex-col gap-4 border-b border-slate-50 p-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-xl font-black tracking-tight text-slate-900">提现记录</h2>
            <p className="mt-1 text-sm font-medium text-slate-500">
              支持按状态查看当前租户的提现申请记录。
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {[
              { label: '全部', value: undefined },
              { label: '待审核', value: 0 },
              { label: '已打款', value: 1 },
              { label: '已驳回', value: 2 },
            ].map((tab) => (
              <button
                key={tab.label}
                onClick={() => setStatusFilter(tab.value)}
                className={cn(
                  'rounded-xl px-4 py-2 text-xs font-black uppercase tracking-widest transition-all',
                  statusFilter === tab.value
                    ? 'bg-primary text-white shadow-lg shadow-primary/20'
                    : 'bg-slate-100 text-slate-500 hover:text-slate-700',
                )}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-4 p-6">
          {withdrawals.length === 0 ? (
            <div className="rounded-[28px] border border-dashed border-slate-200 px-6 py-12 text-center text-sm font-medium text-slate-400">
              当前筛选条件下暂无提现记录。
            </div>
          ) : (
            withdrawals.map((item) => (
              <div
                key={item.id}
                className="rounded-[32px] border border-slate-100 bg-slate-50 p-6"
              >
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div className="flex items-start gap-4">
                    <div
                      className={cn(
                        'rounded-2xl p-3',
                        item.status === 1
                          ? 'bg-green-100 text-green-700'
                          : item.status === 2
                            ? 'bg-red-100 text-red-700'
                            : 'bg-orange-100 text-orange-700',
                      )}
                    >
                      {item.status === 1 ? (
                        <CheckCircle2 className="h-5 w-5" />
                      ) : item.status === 2 ? (
                        <XCircle className="h-5 w-5" />
                      ) : (
                        <Clock className="h-5 w-5" />
                      )}
                    </div>
                    <div>
                      <p className="text-lg font-black tracking-tight text-slate-900">
                        {formatCurrency(item.amount)}
                      </p>
                      <p className="mt-1 text-sm font-medium text-slate-500">
                        {item.bankName} · {maskBankAccount(item.bankAccount)} · {item.accountName}
                      </p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        申请时间：{formatDateTime(item.applyTime || item.createTime)}
                      </p>
                    </div>
                  </div>

                  <div className="flex flex-col items-start gap-2 lg:items-end">
                    <span className={getStatusClassName(item.status)}>
                      {getStatusText(item.status)}
                    </span>
                    {item.approveTime && (
                      <span className="text-xs font-medium text-slate-400">
                        处理时间：{formatDateTime(item.approveTime)}
                      </span>
                    )}
                  </div>
                </div>

                {item.status === 2 && item.rejectReason && (
                  <div className="mt-4 flex items-start gap-2 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
                    <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                    <span>驳回原因：{item.rejectReason}</span>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </section>

      {showForm && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <button
            type="button"
            onClick={() => setShowForm(false)}
            className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"
          />
          <div className="relative z-10 w-full max-w-xl rounded-[40px] bg-white p-8 shadow-2xl">
            <div className="mb-6 flex items-start justify-between gap-4">
              <div>
                <h2 className="text-2xl font-black tracking-tight text-slate-900">发起提现申请</h2>
                <p className="mt-1 text-sm font-medium text-slate-500">
                  当前可提现余额：{formatCurrency(summary.availableBalance)}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowForm(false)}
                className="rounded-2xl bg-slate-100 p-3 text-slate-500 transition-all hover:text-slate-900"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="grid grid-cols-1 gap-5">
              <FormField
                label="提现金额"
                type="number"
                value={String(formData.amount || '')}
                onChange={(value) => updateField('amount', Number(value || 0))}
                placeholder="请输入提现金额"
              />
              <FormField
                label="开户银行"
                value={formData.bankName}
                onChange={(value) => updateField('bankName', value)}
                placeholder="如：招商银行上海分行"
              />
              <FormField
                label="银行卡号"
                value={formData.bankAccount}
                onChange={(value) => updateField('bankAccount', value)}
                placeholder="请输入银行卡号"
              />
              <FormField
                label="账户名称"
                value={formData.accountName}
                onChange={(value) => updateField('accountName', value)}
                placeholder="请输入开户名"
              />
            </div>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-end">
              <button
                type="button"
                onClick={() => setShowForm(false)}
                className="rounded-2xl border border-slate-200 px-6 py-3 text-sm font-black text-slate-600 transition-all hover:border-slate-300"
              >
                取消
              </button>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={isSubmitting}
                className="rounded-2xl bg-primary px-6 py-3 text-sm font-black text-white shadow-xl shadow-primary/20 transition-all hover:scale-105 active:scale-95 disabled:cursor-not-allowed disabled:opacity-70"
              >
                {isSubmitting ? '提交中...' : '提交审核'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function FormField({
  label,
  value,
  onChange,
  placeholder,
  type = 'text',
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  type?: string;
}) {
  return (
    <div className="flex flex-col gap-3">
      <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
        {label}
      </label>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className="w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 px-5 py-4 font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white"
      />
    </div>
  );
}

function getStatusText(status: number) {
  if (status === 1) return '已打款';
  if (status === 2) return '已驳回';
  return '待审核';
}

function getStatusClassName(status: number) {
  return cn(
    'rounded-xl px-3 py-1 text-[10px] font-black uppercase tracking-widest',
    status === 1
      ? 'bg-green-100 text-green-700'
      : status === 2
        ? 'bg-red-100 text-red-700'
        : 'bg-orange-100 text-orange-700',
  );
}

function maskBankAccount(bankAccount?: string | null) {
  if (!bankAccount) {
    return '未提供账户';
  }

  const clean = bankAccount.replace(/\s+/g, '');
  if (clean.length <= 4) {
    return clean;
  }

  return `****${clean.slice(-4)}`;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '--';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}
