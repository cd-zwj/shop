import { useCallback, useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import {
  AlertCircle,
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  CircleDollarSign,
  Download,
  RefreshCw,
  Store,
  Wallet,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { appCatalogService } from '../services/modules/appCatalog';
import { appWalletService } from '../services/modules/appWallet';
import type { WalletAccount, WalletLog } from '../types/wallet';
import { cn } from '../lib/utils';
import { formatCurrency } from '../utils/display';
import { getErrorMessage } from '../utils/errorMessage';
import { getPageTotalPages } from '../utils/pageResult';
import { getWalletLogPresentation } from '../utils/walletLogPresentation';

const PAGE_SIZE = 10;

export default function MerchantWalletDetail() {
  const navigate = useNavigate();
  const { tenantId: tenantIdParam } = useParams<{ tenantId: string }>();
  const tenantId = Number(tenantIdParam);
  const [tenantName, setTenantName] = useState('');
  const [wallet, setWallet] = useState<WalletAccount | null>(null);
  const [logs, setLogs] = useState<WalletLog[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const loadWalletDetail = useCallback(async (isActive: () => boolean = () => true) => {
    if (!tenantId || Number.isNaN(tenantId)) {
      setError('缺少商户参数');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const [tenant, walletInfo, logPage] = await Promise.all([
        appCatalogService.getTenant(tenantId).catch(() => null),
        appWalletService.getMerchantWallet(tenantId),
        appWalletService.getMerchantWalletLogs(tenantId, currentPage, PAGE_SIZE),
      ]);

      if (!isActive()) return;
      setTenantName(tenant?.name ?? `商户 #${tenantId}`);
      setWallet(walletInfo);
      setLogs(logPage.records ?? []);
      setTotalPages(Math.max(1, getPageTotalPages(logPage)));
    } catch (loadError) {
      if (!isActive()) return;
      setError(getErrorMessage(loadError, '商户钱包明细加载失败，请稍后重试'));
      setWallet(null);
      setLogs([]);
      setTotalPages(1);
    } finally {
      if (isActive()) {
        setIsLoading(false);
      }
    }
  }, [currentPage, tenantId]);

  useEffect(() => {
    let isMounted = true;
    void loadWalletDetail(() => isMounted);
    return () => {
      isMounted = false;
    };
  }, [loadWalletDetail, reloadKey]);

  const stats = useMemo(() => {
    const income = logs
      .map((log) => Number(log.changeAmount || 0))
      .filter((amount) => amount > 0)
      .reduce((sum, amount) => sum + amount, 0);
    const expense = logs
      .map((log) => Number(log.changeAmount || 0))
      .filter((amount) => amount < 0)
      .reduce((sum, amount) => sum + Math.abs(amount), 0);

    return { income, expense, count: logs.length };
  }, [logs]);

  function handleRetry() {
    setReloadKey((key) => key + 1);
  }

  function handleExportCsv() {
    if (logs.length === 0) return;
    const header = '时间,业务类型,业务号,变动金额,变更前余额,变更后余额,备注';
    const rows = logs.map((log) => [
      log.createTime ?? '',
      log.bizType ?? '',
      log.bizNo ?? '',
      log.changeAmount ?? 0,
      log.balanceBefore ?? 0,
      log.balanceAfter ?? 0,
      (log.remark ?? '').replace(/,/g, '，'),
    ].join(','));
    const csvContent = '﻿' + [header, ...rows].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `商户钱包流水_${tenantId}_${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-4 pb-12 md:mt-8">
      <header className="flex flex-col gap-4 border-b border-slate-100 pb-5 md:flex-row md:items-end md:justify-between">
        <div className="flex items-start gap-3">
          <button
            type="button"
            onClick={() => navigate(-1)}
            aria-label="返回上一页"
            className="rounded-full p-2 text-slate-600 transition-colors hover:bg-slate-50"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <div className="flex items-center gap-2 text-xs font-black uppercase tracking-widest text-slate-400">
              <Store className="h-4 w-4 text-primary" />
              {tenantName || `商户 #${tenantId || '--'}`}
            </div>
            <h1 className="mt-1 text-3xl font-black tracking-tight text-slate-900">商户钱包明细</h1>
            <p className="mt-1 max-w-2xl text-sm font-medium leading-relaxed text-slate-500">
              追溯该商户钱包的充值、消费、退款和余额变化来源。
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={handleExportCsv}
          disabled={logs.length === 0}
          className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-black text-slate-600 transition-all hover:border-primary/30 hover:text-primary disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Download className="h-4 w-4" />
          导出流水
        </button>
      </header>

      {error && (
        <div className="flex flex-col gap-4 rounded-3xl border border-red-100 bg-red-50 px-6 py-5 text-red-700 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <AlertCircle className="h-5 w-5 flex-none" />
            <span className="text-sm font-bold">{error}</span>
          </div>
          <button
            type="button"
            onClick={handleRetry}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-white px-4 py-2 text-sm font-black text-red-700 shadow-sm transition-all hover:bg-red-100"
          >
            <RefreshCw className="h-4 w-4" />
            重试
          </button>
        </div>
      )}

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="relative overflow-hidden rounded-3xl border border-slate-100 bg-slate-900 p-8 text-white shadow-xl lg:col-span-7"
        >
          <div className="pointer-events-none absolute -right-16 -top-16 h-64 w-64 rounded-full bg-primary/20 blur-3xl" />
          <div className="relative z-10">
            <div className="mb-4 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-slate-400">
              <CircleDollarSign className="h-5 w-5 text-primary-container" />
              商户钱包可用余额
            </div>
            <div className="text-5xl font-black tracking-tight">
              {isLoading ? '...' : formatCurrency(wallet?.availableAmount)}
            </div>
            <p className="mt-4 text-sm font-medium leading-relaxed text-slate-300">
              商户钱包仅在当前商户内使用，充值、订单消费和退款都会进入此明细。
            </p>
          </div>
        </motion.div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:col-span-5">
          <MetricCard label="冻结金额" value={isLoading ? '...' : formatCurrency(wallet?.frozenAmount)} />
          <MetricCard label="累计充值" value={isLoading ? '...' : formatCurrency(wallet?.totalRecharge)} />
          <MetricCard label="累计消费" value={isLoading ? '...' : formatCurrency(wallet?.totalConsume)} />
          <MetricCard label="本页流水" value={isLoading ? '...' : `${stats.count} 笔`} />
        </div>
      </section>

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <SummaryCard label="本页收入" value={formatCurrency(stats.income)} tone="income" />
        <SummaryCard label="本页支出" value={formatCurrency(stats.expense)} tone="expense" />
      </section>

      <section className="overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-xl shadow-slate-200/30">
        <div className="flex items-center justify-between border-b border-slate-50 px-8 py-6">
          <div>
            <h2 className="text-xl font-black text-slate-900">钱包流水</h2>
            <p className="mt-1 text-xs font-bold text-slate-400">点击业务操作可跳转到对应订单、售后或充值状态。</p>
          </div>
        </div>

        <div className="divide-y divide-slate-50">
          {isLoading && Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="flex items-center justify-between px-8 py-6">
              <div className="h-12 w-12 rounded-2xl bg-slate-100" />
              <div className="ml-5 flex-1">
                <div className="h-4 w-40 rounded-full bg-slate-100" />
                <div className="mt-2 h-3 w-64 rounded-full bg-slate-50" />
              </div>
            </div>
          ))}

          {!isLoading && logs.length === 0 && (
            <div className="flex flex-col items-center justify-center px-8 py-16 text-center">
              <Wallet className="mb-4 h-12 w-12 text-slate-200" />
              <h3 className="text-base font-black text-slate-800">暂无商户钱包流水</h3>
              <p className="mt-1 text-sm font-medium text-slate-400">充值、消费或退款后会在这里展示来源明细。</p>
            </div>
          )}

          {!isLoading && logs.map((log, index) => {
            const presentation = getWalletLogPresentation(log);
            const isExpense = presentation.direction === 'expense';
            return (
              <motion.div
                key={`${log.bizNo}-${index}`}
                whileHover={{ backgroundColor: '#f8fafc' }}
                className="group flex flex-col gap-4 px-8 py-6 transition-colors sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="flex items-start gap-5">
                  <div className={cn('flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl text-sm font-black', presentation.badgeClass)}>
                    {presentation.initials}
                  </div>
                  <div>
                    <h3 className="font-black text-slate-900 group-hover:text-primary">{presentation.title}</h3>
                    <p className="mt-1 text-xs font-semibold leading-relaxed text-slate-400">{presentation.source}</p>
                    <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px] font-bold text-slate-400">
                      <span>{log.createTime || '--'}</span>
                      {log.bizNo && <span>业务号 {log.bizNo}</span>}
                    </div>
                    {presentation.actionPath && (
                      <button
                        type="button"
                        onClick={() => navigate(presentation.actionPath!)}
                        className="mt-3 rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-black text-slate-600 transition-all hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                      >
                        {presentation.actionLabel}
                      </button>
                    )}
                  </div>
                </div>
                <div className="text-left sm:text-right">
                  <div className={cn('text-xl font-black tracking-tight', isExpense ? 'text-slate-900' : 'text-primary')}>
                    {presentation.amountText}
                  </div>
                  <div className="mt-1 text-xs font-bold text-slate-400">{presentation.balanceText}</div>
                </div>
              </motion.div>
            );
          })}
        </div>
      </section>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <button
            type="button"
            onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
            disabled={currentPage <= 1}
            aria-label="上一页"
            className="rounded-xl border border-slate-200 p-2 text-slate-600 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ChevronLeft className="h-5 w-5" />
          </button>
          <span className="text-sm font-black text-slate-600">{currentPage} / {totalPages}</span>
          <button
            type="button"
            onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
            disabled={currentPage >= totalPages}
            aria-label="下一页"
            className="rounded-xl border border-slate-200 p-2 text-slate-600 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ChevronRight className="h-5 w-5" />
          </button>
        </div>
      )}
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-3xl border border-slate-100 bg-white p-6 shadow-lg shadow-slate-200/30">
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className="mt-2 text-2xl font-black tracking-tight text-slate-900">{value}</p>
    </div>
  );
}

function SummaryCard({ label, value, tone }: { label: string; value: string; tone: 'income' | 'expense' }) {
  return (
    <div className={cn(
      'rounded-3xl border p-6',
      tone === 'income' ? 'border-emerald-100 bg-emerald-50 text-emerald-700' : 'border-rose-100 bg-rose-50 text-rose-700',
    )}>
      <p className="text-[10px] font-black uppercase tracking-widest opacity-70">{label}</p>
      <p className="mt-2 text-2xl font-black tracking-tight">{value}</p>
    </div>
  );
}
