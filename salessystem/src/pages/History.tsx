import { useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import { Calendar, Download, SearchIcon, Wallet } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { appWalletService } from '../services/modules/appWallet';
import type { WalletLog } from '../types/wallet';
import { formatCurrency } from '../utils/display';
import { getWalletLogPresentation } from '../utils/walletLogPresentation';

export default function ConsumptionHistory() {
  const navigate = useNavigate();
  const [logs, setLogs] = useState<WalletLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [pageSize, setPageSize] = useState(20);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalRecords, setTotalRecords] = useState(0);

  useEffect(() => {
    let isMounted = true;

    async function loadHistory() {
      setIsLoading(true);
      try {
        const result = await appWalletService.getUnifiedWalletLogs(currentPage, pageSize);
        if (!isMounted) return;
        setLogs(result.records ?? []);
        setTotalRecords(result.total ?? 0);
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadHistory();
    return () => {
      isMounted = false;
    };
  }, [currentPage, pageSize]);

  const filteredLogs = useMemo(() => {
    if (!searchTerm.trim()) return logs;
    const term = searchTerm.trim().toLowerCase();
    return logs.filter(
      (item) =>
        (item.bizNo && item.bizNo.toLowerCase().includes(term)) ||
        (item.bizType && item.bizType.toLowerCase().includes(term)) ||
        (item.remark && item.remark.toLowerCase().includes(term)),
    );
  }, [logs, searchTerm]);

  const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));

  const handleExportCsv = () => {
    if (filteredLogs.length === 0) {
      return;
    }
    const header = '时间,业务类型,业务号,变动金额,余额,备注';
    const rows = filteredLogs.map((item) =>
      [
        item.createTime ?? '',
        item.bizType ?? '',
        item.bizNo ?? '',
        item.changeAmount ?? 0,
        item.balanceAfter ?? 0,
        (item.remark ?? '').replace(/,/g, '，'),
      ].join(','),
    );
    const csvContent = '﻿' + [header, ...rows].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `消费记录_${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const stats = useMemo(() => {
    const changes = logs.map((item) => Number(item.changeAmount || 0));
    const totalSpend = changes.filter((value) => value < 0).reduce((sum, value) => sum + Math.abs(value), 0);
    const maxSpend = Math.max(0, ...changes.filter((value) => value < 0).map((value) => Math.abs(value)));
    return {
      totalSpend,
      count: logs.length,
      maxSpend,
    };
  }, [logs]);

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-8 px-4 pb-10 md:mt-8">
      <header className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-4xl font-black tracking-tight text-slate-900">消费记录</h1>
          <p className="mt-2 font-medium text-slate-500">这里展示统一钱包的真实流水记录。</p>
        </div>

        <div className="flex w-full flex-col gap-3 sm:flex-row md:w-auto">
          <div className="group relative">
            <Calendar className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-primary" />
            <select
              value={pageSize}
              onChange={(e) => { setPageSize(Number(e.target.value)); setCurrentPage(1); }}
              className="cursor-pointer appearance-none rounded-2xl border border-slate-200 bg-white py-3 pl-12 pr-10 text-sm font-bold text-slate-700 outline-none transition-all focus:ring-4 focus:ring-primary/5"
            >
              <option value={20}>最近 20 条</option>
              <option value={50}>本月（50条）</option>
              <option value={200}>最近 90 天（200条）</option>
            </select>
          </div>
          <div className="group relative flex-1 sm:w-64">
            <SearchIcon className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-primary" />
            <input
              type="text"
              placeholder="搜索业务号..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full rounded-2xl border border-slate-200 bg-white py-3 pl-12 pr-6 text-sm font-bold text-slate-700 outline-none transition-all focus:ring-4 focus:ring-primary/5"
            />
          </div>
        </div>
      </header>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
        {[
          { label: '累计支出', value: formatCurrency(stats.totalSpend) },
          { label: '流水笔数', value: stats.count.toString() },
          { label: '单笔最大变动', value: formatCurrency(stats.maxSpend) },
        ].map((stat) => (
          <motion.div
            key={stat.label}
            whileHover={{ y: -4 }}
            className="rounded-3xl border border-slate-100 bg-white p-6 shadow-lg shadow-slate-200/30"
          >
            <p className="mb-2 text-[10px] font-black uppercase tracking-widest text-slate-400">{stat.label}</p>
            <p className="text-3xl font-black tracking-tight text-slate-900">{stat.value}</p>
          </motion.div>
        ))}
      </div>

      <section className="overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-2xl shadow-slate-200/40">
        <div className="flex items-center justify-between border-b border-slate-50 px-8 py-6">
          <h3 className="text-lg font-black text-slate-900">最近流水</h3>
          <button
            onClick={handleExportCsv}
            className="flex items-center gap-1.5 rounded-xl px-4 py-2 text-sm font-bold text-primary transition-all hover:bg-primary/5 hover:underline"
          >
            <Download className="h-4 w-4" />
            导出 CSV
          </button>
        </div>

        <div className="divide-y divide-slate-50">
          {(isLoading ? Array.from({ length: 5 }) : filteredLogs).map((item: WalletLog | undefined, index) => {
            const isData = typeof item !== 'undefined';
            const presentation = isData ? getWalletLogPresentation(item) : null;
            const isExpense = presentation?.direction === 'expense';

            return (
              <motion.div
                key={isData ? `${item.bizNo}-${index}` : index}
                whileHover={{ backgroundColor: '#f8fafc' }}
                className="group flex items-center justify-between px-8 py-6 transition-colors"
              >
                <div className="flex items-center gap-6">
                  <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-50 text-slate-500 transition-all group-hover:scale-110">
                    <Wallet className="h-6 w-6" />
                  </div>
                  <div>
                    <h4 className="text-lg font-black text-slate-900 transition-colors group-hover:text-primary">
                      {presentation?.title ?? '加载流水中...'}
                    </h4>
                    <div className="mt-1 flex items-center gap-3">
                      <span className="text-xs font-bold text-slate-400">{isData ? item.createTime || '--' : '--'}</span>
                      <span className="rounded-full border border-slate-200 bg-slate-100 px-2 py-0.5 text-[9px] font-black uppercase tracking-wider text-slate-500">
                        {isData ? presentation?.source : '同步中'}
                      </span>
                    </div>
                    {presentation?.actionPath && (
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

                <div className="text-right">
                  <p className={`text-xl font-black tracking-tight ${isExpense ? 'text-slate-900' : 'text-primary'}`}>
                    {presentation?.amountText ?? '...'}
                  </p>
                  <p className="mt-1 text-xs font-bold text-slate-400">
                    {presentation?.balanceText ?? '正在同步'}
                  </p>
                </div>
              </motion.div>
            );
          })}
        </div>
      </section>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            disabled={currentPage === 1}
            className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-bold text-slate-600 transition-all hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            上一页
          </button>
          <span className="px-3 text-sm font-bold text-slate-500">
            {currentPage} / {totalPages}
          </span>
          <button
            onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
            disabled={currentPage === totalPages}
            className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-bold text-slate-600 transition-all hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            下一页
          </button>
        </div>
      )}
    </div>
  );
}
