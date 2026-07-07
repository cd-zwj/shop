import { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  ArrowRight,
  ArrowUpRight,
  CircleDollarSign,
  Landmark,
  List,
  ShieldCheck,
  Wallet,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { merchantFinanceService } from '../../services/modules/merchantFinance';
import type {
  MerchantPointsRule,
  MerchantRechargeRule,
  MerchantTransaction,
  MerchantWalletSummary,
  MerchantWithdrawal,
} from '../../types/merchant';
import type { PageResult } from '../../types/api';
import { cn } from '../../lib/utils';
import { formatCurrency } from '../../utils/display';
import { Pagination } from '../../components/Pagination';
import { summarizeMerchantTransactions } from '../../utils/merchantFinancePresentation';

const DEFAULT_SUMMARY: MerchantWalletSummary = {
  tenantId: 0,
  availableBalance: 0,
  frozenBalance: 0,
  totalIncome: 0,
  totalWithdrawal: 0,
};

const TX_TYPE_LABEL: Record<string, string> = {
  PAYMENT: '收款',
  REFUND: '退款',
  RECHARGE: '充值',
  WITHDRAWAL: '提现',
  POINTS: '积分',
};

const TX_TYPE_BADGE: Record<string, string> = {
  PAYMENT: 'bg-green-100 text-green-700',
  REFUND: 'bg-red-100 text-red-700',
  RECHARGE: 'bg-blue-100 text-blue-700',
  WITHDRAWAL: 'bg-orange-100 text-orange-700',
  POINTS: 'bg-purple-100 text-purple-700',
};

export default function MerchantFinance() {
  const navigate = useNavigate();
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const [summary, setSummary] = useState<MerchantWalletSummary>(DEFAULT_SUMMARY);
  const [pointsRule, setPointsRule] = useState<MerchantPointsRule | null>(null);
  const [rechargeRules, setRechargeRules] = useState<MerchantRechargeRule[]>([]);
  const [withdrawals, setWithdrawals] = useState<MerchantWithdrawal[]>([]);
  const [transactions, setTransactions] = useState<MerchantTransaction[]>([]);
  const [txTotal, setTxTotal] = useState(0);
  const [txPage, setTxPage] = useState(1);
  const [txTypeFilter, setTxTypeFilter] = useState('');
  const [txStartDate, setTxStartDate] = useState('');
  const [txEndDate, setTxEndDate] = useState('');
  const [txLoading, setTxLoading] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadFinance() {
      if (!tenantId) {
        setError('当前商户会话缺少 tenantId，请重新登录');
        setIsLoading(false);
        return;
      }

      try {
        const [walletSummary, currentPointsRule, currentRechargeRules, withdrawalPage] =
          await Promise.all([
            merchantFinanceService.getWalletSummary(tenantId),
            merchantFinanceService.getPointsRule(tenantId),
            merchantFinanceService.listRechargeRules(tenantId),
            merchantFinanceService.listWithdrawals(tenantId, { current: 1, size: 6 }),
          ]);

        if (!isMounted) return;
        setSummary(walletSummary);
        setPointsRule(currentPointsRule);
        setRechargeRules(currentRechargeRules ?? []);
        setWithdrawals(withdrawalPage.records ?? []);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('商户财务数据加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadFinance();

    return () => {
      isMounted = false;
    };
  }, [tenantId]);

  /* ---------- 收支流水加载 ---------- */
  useEffect(() => {
    if (!tenantId) return undefined;
    let isMounted = true;
    setTxLoading(true);

    merchantFinanceService
      .listTransactions(tenantId, {
        current: txPage,
        size: 10,
        type: txTypeFilter || undefined,
        startDate: txStartDate || undefined,
        endDate: txEndDate || undefined,
      })
      .then((res: PageResult<MerchantTransaction>) => {
        if (!isMounted) return;
        setTransactions(res.records ?? []);
        setTxTotal(res.total ?? 0);
      })
      .catch(() => {
        if (!isMounted) return;
        setTransactions([]);
        setTxTotal(0);
      })
      .finally(() => {
        if (isMounted) setTxLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [tenantId, txPage, txTypeFilter, txStartDate, txEndDate]);

  const enabledRechargeRules = useMemo(
    () => rechargeRules.filter((rule) => Number(rule.status) === 1),
    [rechargeRules],
  );

  const pendingWithdrawals = useMemo(
    () => withdrawals.filter((item) => item.status === 0),
    [withdrawals],
  );

  const transactionSummary = useMemo(
    () => summarizeMerchantTransactions(transactions),
    [transactions],
  );

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end">
        <div>
          <h1 className="text-3xl font-black tracking-tight text-slate-900">商户财务总览</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            {merchantSession
              ? `当前商户：${merchantSession.tenantName}，第 5 部分的余额汇总、规则概览和提现动态已接入真实接口。`
              : '正在同步商户会话...'}
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={() => navigate('/merchant/rules')}
            className="rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm font-black text-slate-700 shadow-sm transition-all hover:border-primary hover:text-primary"
          >
            维护运营规则
          </button>
          <button
            onClick={() => navigate('/merchant/withdrawals')}
            className="flex items-center gap-2 rounded-2xl bg-primary px-6 py-3 text-sm font-black text-white shadow-xl shadow-primary/20 transition-all hover:scale-105 active:scale-95"
          >
            发起提现 <ArrowUpRight className="h-4 w-4" />
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        {[
          {
            label: '可提现余额',
            value: formatCurrency(summary.availableBalance),
            hint: '可直接申请提现',
            icon: Wallet,
            tone: 'bg-white',
          },
          {
            label: '冻结余额',
            value: formatCurrency(summary.frozenBalance),
            hint: '待结算或审核中的资金',
            icon: ShieldCheck,
            tone: 'bg-white',
          },
          {
            label: '累计收入',
            value: formatCurrency(summary.totalIncome),
            hint: '商户总收入',
            icon: CircleDollarSign,
            tone: 'bg-slate-900 text-white',
          },
          {
            label: '累计提现',
            value: formatCurrency(summary.totalWithdrawal),
            hint: `${pendingWithdrawals.length} 笔待审核`,
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
            <p className={cn('text-[10px] font-black uppercase tracking-widest', card.tone.includes('text-white') ? 'text-slate-400' : 'text-slate-400')}>
              {card.label}
            </p>
            <p className="mt-2 text-2xl font-black tracking-tight">{isLoading ? '...' : card.value}</p>
            <p
              className={cn(
                'mt-2 text-xs font-medium',
                card.tone.includes('text-white') ? 'text-slate-400' : 'text-slate-500',
              )}
            >
              {card.hint}
            </p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <section className="flex flex-col gap-6 rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm lg:col-span-7">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-black tracking-tight text-slate-900">运营规则快照</h2>
              <p className="mt-1 text-sm font-medium text-slate-500">
                当前页面展示真实积分比例和充值梯度配置。
              </p>
            </div>
            <button
              onClick={() => navigate('/merchant/rules')}
              className="flex items-center gap-1 text-sm font-black text-primary transition-all hover:gap-2"
            >
              去编辑 <ArrowRight className="h-4 w-4" />
            </button>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="rounded-[28px] bg-slate-50 p-6">
              <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">积分规则</p>
              <p className="mt-3 text-2xl font-black tracking-tight text-slate-900">
                {pointsRule?.enabled ? `${pointsRule.pointsRatio} 积分 / 1 元` : '已停用'}
              </p>
              <p className="mt-2 text-sm font-medium text-slate-500">
                {pointsRule?.enabled
                  ? '每消费 1 元会按当前比例返积分。'
                  : '当前未开启消费返积分。'}
              </p>
            </div>

            <div className="rounded-[28px] bg-slate-50 p-6">
              <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">充值规则</p>
              <p className="mt-3 text-2xl font-black tracking-tight text-slate-900">
                {enabledRechargeRules.length} 个启用梯度
              </p>
              <p className="mt-2 text-sm font-medium text-slate-500">
                共 {rechargeRules.length} 条规则，前台仅会展示启用状态的梯度。
              </p>
            </div>
          </div>

          <div className="flex flex-col gap-3">
            {rechargeRules.length === 0 ? (
              <div className="rounded-[28px] border border-dashed border-slate-200 px-6 py-8 text-center text-sm font-medium text-slate-400">
                当前还没有配置充值规则。
              </div>
            ) : (
              rechargeRules.slice(0, 5).map((rule, index) => (
                <div
                  key={`${rule.id ?? 'new'}-${index}`}
                  className="flex flex-col gap-3 rounded-[28px] border border-slate-100 bg-slate-50 px-6 py-5 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <p className="text-sm font-black text-slate-900">
                      充 {formatCurrency(rule.rechargeAmount)}，送 {formatCurrency(rule.giftAmount)}
                    </p>
                    <p className="mt-1 text-xs font-medium text-slate-500">
                      额外赠送 {rule.giftPoints} 积分
                    </p>
                  </div>
                  <span
                    className={cn(
                      'w-fit rounded-xl px-3 py-1 text-[10px] font-black uppercase tracking-widest',
                      Number(rule.status) === 1
                        ? 'bg-green-100 text-green-700'
                        : 'bg-slate-200 text-slate-500',
                    )}
                  >
                    {Number(rule.status) === 1 ? '启用中' : '已停用'}
                  </span>
                </div>
              ))
            )}
          </div>
        </section>

        <section className="flex flex-col gap-6 lg:col-span-5">
          <div className="rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
            <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">结算提醒</p>
            <p className="mt-4 text-3xl font-black tracking-tight">
              {formatCurrency(summary.availableBalance)}
            </p>
            <p className="mt-2 text-sm font-medium text-slate-400">
              当前可提现余额已从真实接口同步。提现申请会进入管理员审核流程。
            </p>
            <div className="mt-6 grid grid-cols-2 gap-4 border-t border-white/5 pt-6">
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">冻结中</p>
                <p className="mt-1 text-lg font-black">{formatCurrency(summary.frozenBalance)}</p>
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">待审核提现</p>
                <p className="mt-1 text-lg font-black">{pendingWithdrawals.length} 笔</p>
              </div>
            </div>
          </div>

          <div className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
            <div className="mb-5 flex items-center justify-between">
              <h2 className="text-lg font-black tracking-tight text-slate-900">最近提现动态</h2>
              <button
                onClick={() => navigate('/merchant/withdrawals')}
                className="text-sm font-black text-primary"
              >
                查看全部
              </button>
            </div>

            <div className="flex flex-col gap-3">
              {withdrawals.length === 0 ? (
                <div className="rounded-[28px] border border-dashed border-slate-200 px-6 py-8 text-center text-sm font-medium text-slate-400">
                  暂无提现记录。
                </div>
              ) : (
                withdrawals.map((item) => (
                  <div
                    key={item.id}
                    className="rounded-[28px] border border-slate-100 bg-slate-50 px-5 py-4"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <p className="text-sm font-black text-slate-900">
                          {formatCurrency(item.amount)}
                        </p>
                        <p className="mt-1 text-xs font-medium text-slate-500">
                          {item.bankName} · {maskBankAccount(item.bankAccount)}
                        </p>
                        <p className="mt-1 text-[11px] font-medium text-slate-400">
                          {formatDateTime(item.applyTime || item.createTime)}
                        </p>
                      </div>
                      <span className={getWithdrawalStatusClassName(item.status)}>
                        {getWithdrawalStatusText(item.status)}
                      </span>
                    </div>
                    {item.status === 2 && item.rejectReason && (
                      <div className="mt-3 flex items-start gap-2 rounded-2xl border border-red-100 bg-red-50 px-3 py-2 text-xs font-medium text-red-600">
                        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                        <span>驳回原因：{item.rejectReason}</span>
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>

        </section>
      </div>

      {/* 收支流水 */}
      <section className="flex flex-col gap-6 rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-primary/5 flex items-center justify-center text-primary">
              <List className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-xl font-black tracking-tight text-slate-900">收支流水</h2>
              <p className="text-sm font-medium text-slate-500">商户钱包资金变动记录。</p>
            </div>
          </div>
          <div className="flex items-center gap-2 flex-wrap">
            {['', 'PAYMENT', 'REFUND', 'RECHARGE', 'WITHDRAWAL'].map((t) => (
              <button
                key={t}
                onClick={() => {
                  setTxTypeFilter(t);
                  setTxPage(1);
                }}
                className={cn(
                  'rounded-xl px-3 py-1.5 text-xs font-black transition-all',
                  txTypeFilter === t
                    ? 'bg-primary text-white'
                    : 'bg-slate-100 text-slate-500 hover:bg-slate-200',
                )}
              >
                {t === '' ? '全部' : TX_TYPE_LABEL[t] ?? t}
              </button>
            ))}
            <input
              type="date"
              value={txStartDate}
              onChange={(e) => { setTxStartDate(e.target.value); setTxPage(1); }}
              className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-600 bg-white"
              placeholder="开始日期"
            />
            <span className="text-xs text-slate-400">至</span>
            <input
              type="date"
              value={txEndDate}
              onChange={(e) => { setTxEndDate(e.target.value); setTxPage(1); }}
              className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-600 bg-white"
              placeholder="结束日期"
            />
            {(txStartDate || txEndDate) && (
              <button
                onClick={() => { setTxStartDate(''); setTxEndDate(''); setTxPage(1); }}
                className="rounded-xl px-2 py-1.5 text-xs font-bold text-slate-400 hover:text-red-500 transition-colors"
              >
                清除
              </button>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-5">
          {[
            {
              label: '当前页收款',
              value: formatCurrency(transactionSummary.paymentIncome),
              tone: 'text-green-600',
            },
            {
              label: '当前页充值',
              value: formatCurrency(transactionSummary.rechargeIncome),
              tone: 'text-blue-600',
            },
            {
              label: '当前页退款',
              value: formatCurrency(transactionSummary.refundAmount),
              tone: 'text-red-600',
            },
            {
              label: '当前页提现',
              value: formatCurrency(transactionSummary.withdrawalAmount),
              tone: 'text-orange-600',
            },
            {
              label: '当前页净变动',
              value: formatCurrency(transactionSummary.netChange),
              tone: transactionSummary.netChange >= 0 ? 'text-green-600' : 'text-red-600',
            },
          ].map((item) => (
            <div key={item.label} className="rounded-[24px] bg-slate-50 p-4">
              <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">
                {item.label}
              </p>
              <p className={cn('mt-2 text-lg font-black tracking-tight', item.tone)}>
                {item.value}
              </p>
            </div>
          ))}
        </div>

        {txLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-14 rounded-2xl bg-slate-50 animate-pulse" />
            ))}
          </div>
        ) : transactions.length === 0 ? (
          <div className="rounded-[28px] border border-dashed border-slate-200 px-6 py-12 text-center text-sm font-medium text-slate-400">
            暂无收支流水记录。
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100">
                    <th className="pb-3 text-left text-[10px] font-black uppercase tracking-widest text-slate-400">时间</th>
                    <th className="pb-3 text-left text-[10px] font-black uppercase tracking-widest text-slate-400">类型</th>
                    <th className="pb-3 text-left text-[10px] font-black uppercase tracking-widest text-slate-400">关联单号</th>
                    <th className="pb-3 text-right text-[10px] font-black uppercase tracking-widest text-slate-400">变动金额</th>
                    <th className="pb-3 text-right text-[10px] font-black uppercase tracking-widest text-slate-400">变动后余额</th>
                    <th className="pb-3 text-left text-[10px] font-black uppercase tracking-widest text-slate-400">备注</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((tx) => (
                    <tr key={tx.id} className="border-b border-slate-50 hover:bg-slate-50/50 transition-colors">
                      <td className="py-3.5 text-slate-600 font-medium">
                        {tx.createTime ? formatDateTime(tx.createTime) : '--'}
                      </td>
                      <td className="py-3.5">
                        <span className={cn('rounded-xl px-2.5 py-1 text-[10px] font-black uppercase tracking-widest', TX_TYPE_BADGE[tx.bizType] ?? 'bg-slate-100 text-slate-500')}>
                          {TX_TYPE_LABEL[tx.bizType] ?? tx.bizType}
                        </span>
                      </td>
                      <td className="py-3.5 font-mono text-xs text-slate-500">{tx.bizNo ?? '--'}</td>
                      <td className={cn('py-3.5 text-right font-black', (tx.changeAmount ?? 0) >= 0 ? 'text-green-600' : 'text-red-600')}>
                        {(tx.changeAmount ?? 0) >= 0 ? '+' : ''}{formatCurrency(tx.changeAmount)}
                      </td>
                      <td className="py-3.5 text-right font-medium text-slate-600">
                        {tx.balanceAfter != null ? formatCurrency(tx.balanceAfter) : '--'}
                      </td>
                      <td className="py-3.5 text-slate-500 max-w-[200px] truncate">{tx.remark ?? '--'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Pagination
              current={txPage}
              total={txTotal}
              pageSize={10}
              onChange={setTxPage}
              className="pt-2"
            />
          </>
        )}
      </section>
    </div>
  );
}

function getWithdrawalStatusText(status: number) {
  if (status === 1) return '已打款';
  if (status === 2) return '已驳回';
  return '待审核';
}

function getWithdrawalStatusClassName(status: number) {
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
