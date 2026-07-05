import { useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  ArrowUpRight,
  Clock,
  CreditCard,
  Package,
  Plus,
  TrendingUp,
  Users,
} from 'lucide-react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { merchantOrderService } from '../../services/modules/merchantOrder';
import { merchantProductService } from '../../services/modules/merchantProduct';
import { merchantRefundService } from '../../services/modules/merchantRefund';
import type { MerchantOrder, MerchantProduct } from '../../types/merchant';
import type { Refund } from '../../types/refund';
import { cn } from '../../lib/utils';
import { formatCurrency } from '../../utils/display';
import { getErrorMessage } from '../../utils/errorMessage';
import { buildMerchantWorkItems, getOrderToneClass, prioritizeMerchantWorkItems } from '../../utils/orderLifecycle';

export default function MerchantDashboard() {
  const navigate = useNavigate();
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const [products, setProducts] = useState<MerchantProduct[]>([]);
  const [orders, setOrders] = useState<MerchantOrder[]>([]);
  const [refunds, setRefunds] = useState<Refund[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let isMounted = true;

    async function loadDashboard() {
      setIsLoading(true);
      if (!tenantId) {
        setError('当前商户会话缺少 tenantId，请重新登录');
        setIsLoading(false);
        return;
      }

      try {
        const [productPage, orderPage, refundPage] = await Promise.all([
          merchantProductService.listProducts(tenantId, { current: 1, size: 50 }),
          merchantOrderService.listOrders(tenantId, { current: 1, size: 50 }),
          merchantRefundService.listRefunds(tenantId, undefined, 1, 50),
        ]);

        if (!isMounted) return;
        setProducts(productPage.records ?? []);
        setOrders(orderPage.records ?? []);
        setRefunds(refundPage.records ?? []);
      } catch (loadError) {
        if (!isMounted) return;
        setProducts([]);
        setOrders([]);
        setRefunds([]);
        setError(getErrorMessage(loadError, '商户仪表盘数据加载失败，请稍后重试'));
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
  }, [tenantId, reloadKey]);

  const totalSales = useMemo(
    () => orders.reduce((sum, order) => sum + Number(order.totalAmount || 0), 0),
    [orders],
  );
  const pendingOrders = useMemo(
    () => orders.filter((order) => order.orderStatus === 'CREATED' || (order.orderStatus === 'PAID' && order.payStatus === 'SUCCESS')).length,
    [orders],
  );
  const activeProducts = useMemo(
    () => products.filter((product) => product.status === 'active').length,
    [products],
  );
  const lowStockProducts = useMemo(
    () => products.filter((product) => Number(product.stock || 0) <= 5).length,
    [products],
  );

  const salesData = useMemo(() => {
    const dailyTotals = new Map<string, number>();
    orders.forEach((order) => {
      const key = (order.createTime || '').slice(5, 10) || order.orderNo.slice(-5);
      dailyTotals.set(key, (dailyTotals.get(key) ?? 0) + Number(order.totalAmount || 0));
    });
    return Array.from(dailyTotals.entries())
      .slice(-7)
      .map(([day, sales]) => ({ day, sales }));
  }, [orders]);
  const workItems = useMemo(
    () => prioritizeMerchantWorkItems(buildMerchantWorkItems({ orders, products, refunds })),
    [orders, products, refunds],
  );
  const totalWorkItemCount = useMemo(
    () => workItems.reduce((sum, item) => sum + item.count, 0),
    [workItems],
  );

  return (
    <div className="flex flex-col gap-6 p-4 md:gap-8 md:p-8">
      <header className="flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-3xl font-black tracking-tight text-slate-900">商户工作台</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            {merchantSession
              ? `当前商户：${merchantSession.tenantName}，真实商品和订单数据已经接入。`
              : '正在同步商户会话...'}
          </p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => navigate('/merchant/product/new')}
            className="flex flex-1 items-center justify-center gap-2 rounded-2xl bg-primary px-6 py-4 text-sm font-black text-white shadow-xl shadow-primary/20 transition-all hover:scale-105 active:scale-95 sm:flex-none"
          >
            <Plus className="h-4 w-4" /> 发布新商品
          </button>
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

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 md:gap-6">
        {[
          {
            label: '累计订单金额',
            value: isLoading ? '...' : formatCurrency(totalSales),
            trend: '订单总额',
            isUp: true,
            icon: TrendingUp,
          },
          {
            label: '待处理订单',
            value: isLoading ? '...' : pendingOrders.toString(),
            trend: '待跟进',
            isUp: false,
            icon: Clock,
          },
          {
            label: '上架商品数',
            value: isLoading ? '...' : activeProducts.toString(),
            trend: `${lowStockProducts} 个低库存`,
            isUp: true,
            icon: Package,
          },
          {
            label: '支付成功订单',
            value: isLoading
              ? '...'
              : orders.filter((order) => order.payStatus === 'SUCCESS').length.toString(),
            trend: '真实支付状态',
            isUp: true,
            icon: Users,
          },
        ].map((stat) => (
          <div
            key={stat.label}
            className="group flex flex-col gap-4 rounded-[32px] border border-slate-100 bg-white p-6 shadow-xl shadow-slate-200/40"
          >
            <div className="flex items-start justify-between">
              <div className="rounded-2xl bg-slate-50 p-4 transition-all group-hover:bg-primary/5 group-hover:text-primary">
                <stat.icon className="h-6 w-6" />
              </div>
              <span
                className={cn(
                  'rounded-lg px-2 py-1 text-[10px] font-black uppercase tracking-wider',
                  stat.isUp
                    ? 'border border-green-100 bg-green-50 text-green-600'
                    : 'border border-orange-100 bg-orange-50 text-orange-600',
                )}
              >
                {stat.trend}
              </span>
            </div>
            <div>
              <p className="mb-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                {stat.label}
              </p>
              <p className="text-2xl font-black tracking-tight text-slate-900 md:text-3xl">
                {stat.value}
              </p>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12 md:gap-8">
        <div className="rounded-[32px] border border-slate-100 bg-white p-6 shadow-xl shadow-slate-200/30 md:rounded-[40px] md:p-10 lg:col-span-8">
          <div className="mb-10 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
            <h3 className="text-sm font-black uppercase tracking-widest italic text-slate-900">
              最近订单金额走势
            </h3>
            <span className="w-fit rounded-xl bg-slate-50 px-4 py-2.5 text-[10px] font-black uppercase tracking-widest text-slate-500">
              基于最近 50 条订单
            </span>
          </div>
          <div className="-ml-4 h-[250px] w-full md:h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={salesData}>
                <defs>
                  <linearGradient id="merchantSalesGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#003d9b" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#003d9b" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid vertical={false} stroke="#f1f5f9" strokeDasharray="3 3" />
                <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fontSize: 10, fontWeight: 700, fill: '#94a3b8' }} />
                <YAxis hide />
                <Tooltip
                  contentStyle={{
                    borderRadius: '16px',
                    border: 'none',
                    boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
                    fontWeight: 'bold',
                    fontSize: '11px',
                  }}
                  formatter={(value: number) => [formatCurrency(value), '订单金额']}
                />
                <Area
                  dataKey="sales"
                  type="monotone"
                  stroke="#003d9b"
                  strokeWidth={4}
                  fillOpacity={1}
                  fill="url(#merchantSalesGradient)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="flex flex-col gap-6 md:gap-8 lg:col-span-4">
          <div className="group relative overflow-hidden rounded-[32px] bg-slate-900 p-8 text-white shadow-2xl md:rounded-[40px] md:p-10">
            <div className="absolute right-0 top-0 -mr-8 -mt-8 rotate-12 opacity-10 transition-transform duration-1000 group-hover:scale-125">
              <CreditCard size={160} />
            </div>
            <h3 className="relative z-10 mb-2 text-xs font-black uppercase tracking-widest text-slate-500">
              当前商户
            </h3>
            <div className="relative z-10 mb-3 text-xl font-black tracking-tight">
              {merchantSession?.tenantName || '商户会话同步中'}
            </div>
            <div className="relative z-10 mb-8 text-sm font-medium text-slate-400">
              角色：{merchantSession?.employeeRole || '--'}
            </div>
            <button
              onClick={() => navigate('/merchant/orders')}
              className="relative z-10 flex w-full items-center justify-center gap-3 rounded-[20px] bg-white py-5 text-sm font-black text-slate-900 transition-all hover:bg-slate-100 active:scale-95"
            >
              进入订单管理 <ArrowRight size={18} />
            </button>
          </div>

          <div className="flex flex-col gap-6 rounded-[32px] border border-slate-100 bg-white p-6 shadow-sm md:rounded-[40px] md:p-8">
            <div className="flex items-center justify-between">
              <h3 className="text-[10px] font-black uppercase tracking-widest text-slate-400">
                今日待办中心
              </h3>
              <span className="rounded-full bg-slate-900 px-2.5 py-1 text-[10px] font-black text-white">
                {isLoading ? '...' : `${totalWorkItemCount} 项`}
              </span>
            </div>
            <div className="flex flex-col gap-4">
              {workItems.map((item) => (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => navigate(item.path)}
                  className={cn(
                    'overflow-hidden rounded-2xl border p-5 text-left transition-all hover:bg-white hover:shadow-xl hover:shadow-slate-100',
                    item.count > 0
                      ? 'border-transparent bg-slate-50'
                      : 'border-slate-100 bg-white opacity-70',
                  )}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3 pr-2">
                      <div
                        className={cn(
                          'h-2 w-2 shrink-0 rounded-full',
                          item.tone === 'blue' ? 'bg-blue-500' : item.tone === 'red' ? 'bg-red-500' : 'bg-orange-500',
                        )}
                      />
                      <span className="line-clamp-1 text-xs font-bold text-slate-800">
                        {isLoading ? '...' : item.count} 个{item.label}
                      </span>
                    </div>
                    <span className={cn('rounded-lg border px-2 py-1 text-[10px] font-black', item.count > 0 ? getOrderToneClass(item.tone) : 'border-slate-200 bg-slate-50 text-slate-400')}>
                      {item.count > 0 ? '处理' : '查看'}
                    </span>
                  </div>
                  <p className="mt-2 line-clamp-2 text-xs font-medium leading-relaxed text-slate-400">
                    {item.description}
                  </p>
                  <ArrowUpRight className="mt-3 h-4 w-4 shrink-0 text-slate-300 transition-colors" />
                </button>
              ))}
            </div>
            <button
              onClick={() => navigate('/merchant/products')}
              className="w-full rounded-2xl border-2 border-slate-100 py-4 text-[10px] font-black uppercase tracking-widest text-slate-400 transition-all hover:border-primary hover:text-primary"
            >
              进入商品管理中心
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
