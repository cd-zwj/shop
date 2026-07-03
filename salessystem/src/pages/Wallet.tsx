import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { motion } from 'motion/react';
import {
  ChevronRight,
  CircleDollarSign,
  Plus,
  Receipt,
  Star,
  Ticket,
  TrendingUp,
  Wallet as WalletIcon,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { appCatalogService } from '../services/modules/appCatalog';
import { appWalletService } from '../services/modules/appWallet';
import type { Tenant } from '../types/catalog';
import type { WalletAccount, WalletLog } from '../types/wallet';
import { cn } from '../lib/utils';
import { formatCurrency } from '../utils/display';

export default function UserWallet() {
  const navigate = useNavigate();
  const [wallet, setWallet] = useState<WalletAccount | null>(null);
  const [logs, setLogs] = useState<WalletLog[]>([]);
  const [points, setPoints] = useState<number | null>(null);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadWalletData() {
      try {
        const [walletInfo, walletLogs, merchantList] = await Promise.all([
          appWalletService.getUnifiedWallet(),
          appWalletService.getUnifiedWalletLogs(1, 5),
          appCatalogService.listTenants(),
        ]);

        if (!isMounted) return;
        setWallet(walletInfo);
        setLogs(walletLogs.records ?? []);
        setTenants(merchantList.slice(0, 3));

        if (merchantList[0]) {
          try {
            const pointsAccount = await appWalletService.getPointsAccount(merchantList[0].id);
            if (!isMounted) return;
            setPoints(pointsAccount.points);
          } catch {
            if (!isMounted) return;
            setPoints(null);
          }
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadWalletData();

    return () => {
      isMounted = false;
    };
  }, []);

  const recentDistribution = useMemo(() => {
    if (logs.length === 0) {
      return [];
    }

    const totalChange = logs.reduce((sum, log) => sum + Math.abs(Number(log.changeAmount || 0)), 0) || 1;
    return logs.slice(0, 3).map((log, index) => ({
      name: log.bizType || `流水 ${index + 1}`,
      type: log.remark || log.bizNo,
      amount: formatCurrency(log.changeAmount),
      percent: `${Math.round((Math.abs(Number(log.changeAmount || 0)) / totalChange) * 100)}%`,
      color:
        index === 0
          ? 'bg-primary/10 text-primary'
          : index === 1
            ? 'bg-secondary/10 text-secondary'
            : 'bg-slate-100 text-slate-600',
      abbr: (log.bizType || 'WL').slice(0, 2).toUpperCase(),
    }));
  }, [logs]);

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-4 pb-10 md:mt-8">
      <header>
        <h1 className="text-3xl font-black text-slate-900">我的钱包</h1>
        <p className="mt-1 font-medium text-slate-500">这里的数据已经来自真实钱包、积分和流水接口。</p>
      </header>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="relative col-span-8 flex min-h-[280px] flex-col justify-between overflow-hidden rounded-3xl border border-slate-100 bg-white p-8 shadow-xl shadow-slate-200/50"
        >
          <div className="pointer-events-none absolute -right-20 -top-20 h-80 w-80 rounded-full bg-primary/5 blur-3xl" />

          <div className="relative z-10">
            <div className="mb-4 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-slate-400">
              <CircleDollarSign className="h-5 w-5 text-primary" />
              统一钱包余额
            </div>
            <div className="text-5xl font-black tracking-tight text-slate-900">
              {isLoading ? '...' : formatCurrency(wallet?.availableAmount)}
            </div>
            <div className="mt-6 inline-flex items-center gap-1.5 rounded-full border border-green-100 bg-green-50 px-3 py-1.5 text-xs font-black text-green-600">
              <TrendingUp className="h-4 w-4" />
              已接入真实后端
            </div>
          </div>

          <div className="relative z-10 mt-10 flex flex-wrap gap-12 border-t border-slate-50 pt-8">
            <div className="min-w-[140px] flex-1">
              <div className="text-xs font-black uppercase tracking-widest text-slate-400">可用资金</div>
              <div className="mt-1 text-2xl font-black tracking-tight text-slate-900">
                {isLoading ? '...' : formatCurrency(wallet?.availableAmount)}
              </div>
            </div>
            <div className="min-w-[140px] flex-1">
              <div className="text-xs font-black uppercase tracking-widest text-slate-400">冻结金额</div>
              <div className="mt-1 text-2xl font-black tracking-tight text-slate-900">
                {isLoading ? '...' : formatCurrency(wallet?.frozenAmount)}
              </div>
            </div>
          </div>
        </motion.div>

        <div className="col-span-4 flex flex-col gap-6">
          <MetricCard
            label="积分"
            value={points === null ? '--' : points.toLocaleString()}
            accent="text-tertiary"
            bg="bg-tertiary/5"
            icon={<Star className="h-6 w-6 fill-current" />}
          />
          <MetricCard
            label="接入商户"
            value={tenants.length.toString()}
            accent="text-primary"
            bg="bg-primary/5"
            icon={<Ticket className="h-6 w-6 fill-current" />}
          />
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4 md:gap-6">
        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={() => navigate('/recharge')}
          className="flex flex-col items-center justify-center gap-3 rounded-3xl bg-primary px-4 py-6 text-white shadow-xl shadow-primary/20 transition-all hover:bg-primary-container"
        >
          <Plus className="h-6 w-6" />
          <span className="text-sm font-black uppercase tracking-widest">充值</span>
        </motion.button>

        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={() => navigate('/history')}
          className="flex flex-col items-center justify-center gap-3 rounded-3xl border border-slate-200 bg-white px-4 py-6 text-slate-900 shadow-sm transition-all hover:bg-slate-50"
        >
          <Receipt className="h-6 w-6 text-slate-400" />
          <span className="text-sm font-black uppercase tracking-widest">交易记录</span>
        </motion.button>

        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={() => navigate('/orders')}
          className="flex flex-col items-center justify-center gap-3 rounded-3xl border border-slate-200 bg-white px-4 py-6 text-slate-900 shadow-sm transition-all hover:bg-slate-50"
        >
          <WalletIcon className="h-6 w-6 text-slate-400" />
          <span className="text-sm font-black uppercase tracking-widest">我的订单</span>
        </motion.button>
      </div>

      <section className="overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-xl shadow-slate-200/30">
        <div className="flex items-center justify-between border-b border-slate-50 px-8 py-6">
          <h3 className="text-xl font-black text-slate-900">最近钱包流水</h3>
          <button onClick={() => navigate('/history')} className="flex items-center gap-1 text-sm font-bold text-primary hover:underline">
            查看全部 <ChevronRight className="h-4 w-4" />
          </button>
        </div>

        <div className="divide-y divide-slate-50">
          {(recentDistribution.length > 0 ? recentDistribution : [{
            name: '暂无流水',
            type: '等待真实数据',
            amount: '$0.00',
            percent: '0%',
            color: 'bg-slate-100 text-slate-600',
            abbr: 'NA',
          }]).map((entry, index) => (
            <motion.div
              key={`${entry.name}-${index}`}
              whileHover={{ backgroundColor: '#f8fafc' }}
              className="flex items-center justify-between px-8 py-5 transition-colors"
            >
              <div className="flex items-center gap-5">
                <div className={cn('flex h-12 w-12 items-center justify-center rounded-2xl font-black text-sm shadow-sm', entry.color)}>
                  {entry.abbr}
                </div>
                <div>
                  <div className="font-black text-slate-900">{entry.name}</div>
                  <div className="mt-0.5 text-xs font-semibold text-slate-400">{entry.type}</div>
                </div>
              </div>
              <div className="text-right">
                <div className="font-black text-slate-900">{entry.amount}</div>
                <div className="mt-0.5 text-xs font-semibold text-slate-400">占变动的 {entry.percent}</div>
              </div>
            </motion.div>
          ))}
        </div>
      </section>
    </div>
  );
}

function MetricCard({
  label,
  value,
  accent,
  bg,
  icon,
}: {
  label: string;
  value: string;
  accent: string;
  bg: string;
  icon: ReactNode;
}) {
  return (
    <motion.div
      whileHover={{ y: -4, boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.05)' }}
      className="flex flex-1 items-center justify-between rounded-3xl border border-slate-100 bg-white p-6 shadow-lg shadow-slate-200/40"
    >
      <div>
        <div className="text-xs font-black uppercase tracking-widest text-slate-400">{label}</div>
        <div className={cn('mt-2 text-3xl font-black tracking-tight', accent)}>{value}</div>
      </div>
      <div className={cn('flex h-14 w-14 items-center justify-center rounded-2xl', bg, accent)}>{icon}</div>
    </motion.div>
  );
}
