import { useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  ArrowRight,
  CreditCard,
  Receipt,
  ShieldCheck,
  ShoppingBag,
  Store,
  Users,
  Wallet,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { adminDashboardService } from '../services/modules/adminDashboard';
import type { AdminDashboardOverview, AdminInfo } from '../types/admin';
import { formatCurrency } from '../utils/display';
import { getErrorMessage } from '../utils/errorMessage';

const EMPTY_OVERVIEW: AdminDashboardOverview = {
  totalPlatformUsers: 0,
  totalMerchants: 0,
  activeMerchants: 0,
  totalOrders: 0,
  paidOrders: 0,
  totalOrderAmount: 0,
  totalPaymentBills: 0,
  totalPaymentAmount: 0,
  totalRechargeOrders: 0,
  totalRechargeAmount: 0,
  pendingWithdrawals: 0,
};

export default function AdminDashboard() {
  const navigate = useNavigate();
  const { adminSession } = useAuth();
  const [info, setInfo] = useState<AdminInfo | null>(null);
  const [overview, setOverview] = useState<AdminDashboardOverview>(EMPTY_OVERVIEW);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let isMounted = true;

    async function loadDashboard() {
      setIsLoading(true);
      try {
        const [nextInfo, nextOverview] = await Promise.all([
          adminDashboardService.getInfo(),
          adminDashboardService.getOverview(),
        ]);
        if (!isMounted) return;
        setInfo(nextInfo);
        setOverview(nextOverview);
        setError('');
      } catch (loadError) {
        if (!isMounted) return;
        setError(getErrorMessage(loadError, '管理端总览数据加载失败，请稍后重试'));
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadDashboard();

    return () => {
      isMounted = false;
    };
  }, [reloadKey]);

  const paymentSuccessRate = useMemo(() => {
    if (!overview.totalOrders) {
      return 0;
    }
    return (Number(overview.paidOrders || 0) / Number(overview.totalOrders || 1)) * 100;
  }, [overview.paidOrders, overview.totalOrders]);

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-col justify-between gap-6 md:flex-row md:items-end">
        <div>
          <h1 className="text-3xl font-black tracking-tight text-slate-900">管理端业务总览</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            {info?.nickname || info?.username || adminSession?.username
              ? `当前管理员：${info?.nickname || info?.username || adminSession?.username}，第 6 部分的认证、总览和治理页已切到真实接口。`
              : '正在同步管理端会话与总览数据...'}
          </p>
        </div>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          <div className="flex items-center justify-between gap-4">
            <span>{error}</span>
            <button type="button" onClick={() => setReloadKey((key) => key + 1)} className="shrink-0 font-black text-red-700">
              重试
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          {
            label: '平台用户数',
            value: overview.totalPlatformUsers.toString(),
            hint: `商户员工关系 ${overview.activeMerchants} 个活跃商户`,
            icon: Users,
          },
          {
            label: '商户数量',
            value: overview.totalMerchants.toString(),
            hint: `${overview.activeMerchants} 个启用中`,
            icon: Store,
          },
          {
            label: '订单总额',
            value: formatCurrency(overview.totalOrderAmount),
            hint: `${overview.totalOrders} 笔订单`,
            icon: ShoppingBag,
          },
          {
            label: '待审核提现',
            value: overview.pendingWithdrawals.toString(),
            hint: `${overview.totalRechargeOrders} 笔充值订单`,
            icon: Wallet,
          },
        ].map((card) => (
          <div
            key={card.label}
            className="rounded-[32px] border border-slate-100 bg-white p-6 shadow-xl shadow-slate-200/30"
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
            <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">
              {card.label}
            </p>
            <p className="mt-2 text-2xl font-black tracking-tight text-slate-900">
              {isLoading ? '...' : card.value}
            </p>
            <p className="mt-2 text-sm font-medium text-slate-500">{card.hint}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm lg:col-span-8">
          <div className="mb-8 flex items-center justify-between">
            <div>
              <h2 className="text-xl font-black tracking-tight text-slate-900">核心交易指标</h2>
              <p className="mt-1 text-sm font-medium text-slate-500">
                数据来自 `/v1/admin/dashboard/overview`
              </p>
            </div>
            <button
              onClick={() => navigate('/admin/transactions')}
              className="flex items-center gap-1 text-sm font-black text-primary transition-all hover:gap-2"
            >
              查看交易 <ArrowRight className="h-4 w-4" />
            </button>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <MetricPanel
              title="支付单金额"
              value={formatCurrency(overview.totalPaymentAmount)}
              hint={`${overview.totalPaymentBills} 笔支付单`}
              icon={<CreditCard className="h-5 w-5 text-primary" />}
            />
            <MetricPanel
              title="充值总额"
              value={formatCurrency(overview.totalRechargeAmount)}
              hint={`${overview.totalRechargeOrders} 笔充值单`}
              icon={<Receipt className="h-5 w-5 text-primary" />}
            />
            <MetricPanel
              title="支付成功订单"
              value={overview.paidOrders.toString()}
              hint={`成功率 ${paymentSuccessRate.toFixed(1)}%`}
              icon={<ShieldCheck className="h-5 w-5 text-primary" />}
            />
            <MetricPanel
              title="管理员权限数"
              value={String(info?.permissions?.length ?? adminSession?.permissions?.length ?? 0)}
              hint={`角色 ${info?.roles?.length ?? adminSession?.roles?.length ?? 0} 个`}
              icon={<Users className="h-5 w-5 text-primary" />}
            />
          </div>
        </section>

        <section className="flex flex-col gap-6 lg:col-span-4">
          <div className="rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
            <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">管理员会话</p>
            <p className="mt-4 text-2xl font-black tracking-tight">
              {info?.nickname || info?.username || '--'}
            </p>
            <p className="mt-2 text-sm font-medium text-slate-400">
              作用域：{info?.scope || adminSession?.scope || '--'}
            </p>
            <div className="mt-6 grid grid-cols-2 gap-4 border-t border-white/5 pt-6">
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">角色数</p>
                <p className="mt-1 text-lg font-black">
                  {info?.roles?.length ?? adminSession?.roles?.length ?? 0}
                </p>
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">权限数</p>
                <p className="mt-1 text-lg font-black">
                  {info?.permissions?.length ?? adminSession?.permissions?.length ?? 0}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
            <h2 className="text-lg font-black tracking-tight text-slate-900">快速治理入口</h2>
            <div className="mt-5 flex flex-col gap-3">
              {[
                { label: '商户管理', path: '/admin/merchants' },
                { label: '用户管理', path: '/admin/users' },
                { label: '权限中心', path: '/admin/permissions' },
                { label: '提现审核', path: '/admin/withdrawals' },
              ].map((item) => (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  className="flex items-center justify-between rounded-[24px] bg-slate-50 px-5 py-4 text-left transition-all hover:bg-slate-100"
                >
                  <span className="text-sm font-black text-slate-800">{item.label}</span>
                  <ArrowRight className="h-4 w-4 text-slate-400" />
                </button>
              ))}
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

function MetricPanel({
  title,
  value,
  hint,
  icon,
}: {
  title: string;
  value: string;
  hint: string;
  icon: ReactNode;
}) {
  return (
    <div className="rounded-[28px] bg-slate-50 p-6">
      <div className="mb-4 flex items-center gap-3">
        <div className="rounded-2xl bg-white p-3 shadow-sm">{icon}</div>
        <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{title}</p>
      </div>
      <p className="text-2xl font-black tracking-tight text-slate-900">{value}</p>
      <p className="mt-2 text-sm font-medium text-slate-500">{hint}</p>
    </div>
  );
}
