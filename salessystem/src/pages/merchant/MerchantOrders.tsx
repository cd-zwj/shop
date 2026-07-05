import { useEffect, useMemo, useState } from 'react';
import {
  ArrowUpDown,
  CheckCircle2,
  Clock,
  Eye,
  Filter,
  Package,
  Search,
  Truck,
  XCircle,
} from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { merchantOrderService } from '../../services/modules/merchantOrder';
import type { MerchantOrder } from '../../types/merchant';
import { cn } from '../../lib/utils';
import { formatCurrency } from '../../utils/display';
import { getOrderLifecyclePresentation, getOrderToneClass } from '../../utils/orderLifecycle';

const ORDER_TABS = [
  { id: 'all', label: '全部订单' },
  { id: 'pending', label: '待付款' },
  { id: 'shipping', label: '待履约' },
  { id: 'completed', label: '已完成' },
  { id: 'abnormal', label: '异常订单' },
] as const;

type MerchantOrderTab = typeof ORDER_TABS[number]['id'];

function normalizeOrderTab(tab: string | null): MerchantOrderTab {
  return ORDER_TABS.some((item) => item.id === tab) ? tab as MerchantOrderTab : 'all';
}

export default function MerchantOrders() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const [activeTab, setActiveTab] = useState<MerchantOrderTab>(() => normalizeOrderTab(searchParams.get('tab')));
  const [keyword, setKeyword] = useState('');
  const [orders, setOrders] = useState<MerchantOrder[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const tabFilters = useMemo(
    () => ({
      all: {},
      pending: { orderStatus: 'CREATED' },
      shipping: { payStatus: 'SUCCESS' },
      completed: { orderStatus: 'CLOSED' },
      abnormal: { payStatus: 'FAILED' },
    }),
    [],
  );

  useEffect(() => {
    const nextTab = normalizeOrderTab(searchParams.get('tab'));
    setActiveTab((current) => current === nextTab ? current : nextTab);
  }, [searchParams]);

  const handleTabChange = (tabId: MerchantOrderTab) => {
    setActiveTab(tabId);
    if (tabId === 'all') {
      setSearchParams({});
      return;
    }
    setSearchParams({ tab: tabId });
  };

  useEffect(() => {
    let isMounted = true;

    async function loadOrders() {
      if (!tenantId) {
        setError('当前商户会话缺少 tenantId，请重新登录');
        setIsLoading(false);
        return;
      }

      try {
        const result = await merchantOrderService.listOrders(tenantId, {
          current: 1,
          size: 100,
          keyword: keyword.trim() || undefined,
          ...tabFilters[activeTab],
        });
        if (!isMounted) return;
        setOrders(result.records ?? []);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('订单列表加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadOrders();

    return () => {
      isMounted = false;
    };
  }, [activeTab, keyword, tabFilters, tenantId]);

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-col justify-between gap-6 md:flex-row md:items-center">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-black tracking-tight text-slate-900">订单履约中心</h1>
          <p className="font-medium text-slate-500">当前商户订单列表已切换为真实接口数据。</p>
        </div>
        <div className="flex items-center gap-3">
          <button className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-bold text-slate-700 shadow-sm transition-all hover:bg-slate-50">
            <ArrowUpDown className="h-4 w-4" /> 导出 CSV
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="w-fit rounded-2xl bg-slate-100 p-1">
        {ORDER_TABS.map((tab) => (
          <button
            key={tab.id}
            onClick={() => handleTabChange(tab.id)}
            className={cn(
              'rounded-xl px-6 py-2.5 text-xs font-black uppercase tracking-widest transition-all',
              activeTab === tab.id ? 'bg-white text-primary shadow-sm' : 'text-slate-400 hover:text-slate-600',
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="flex flex-col overflow-hidden rounded-[40px] border border-slate-100 bg-white shadow-xl shadow-slate-200/40">
        <div className="flex items-center gap-4 border-b border-slate-50 p-6">
          <div className="relative flex-1">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="搜索订单号、订单主题..."
              className="w-full rounded-2xl border-none bg-slate-50 py-3 pl-11 pr-4 text-sm font-medium outline-none transition-all focus:bg-white focus:ring-4 focus:ring-primary/5"
            />
          </div>
          <button className="rounded-2xl bg-slate-50 p-3 text-slate-400 transition-all hover:text-primary">
            <Filter size={20} />
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-slate-50/50">
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest italic text-slate-400">
                  订单信息
                </th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest italic text-slate-400">
                  商户/用户
                </th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest italic text-slate-400">
                  订单金额
                </th>
                <th className="px-8 py-5 text-center text-[10px] font-black uppercase tracking-widest italic text-slate-400">
                  当前进度
                </th>
                <th className="px-8 py-5"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {(isLoading ? Array.from<MerchantOrder | undefined>({ length: 4 }) : orders).map((order, index) => {                const lifecycle = getOrderLifecyclePresentation(order);
                return (
                  <tr
                    key={order ? order.orderNo : index}
                    className="group cursor-pointer transition-colors hover:bg-slate-50/50"
                    onClick={() => order && navigate(`/merchant/order/${order.orderNo}`)}
                  >
                    <td className="px-8 py-6">
                      <div className="flex items-center gap-5">
                        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl border border-slate-100 bg-white shadow-inner transition-transform group-hover:scale-100">
                          <Package className="h-6 w-6 text-slate-300" />
                        </div>
                        <div className="flex flex-col">
                          <span className="font-mono text-xs font-black tracking-tight text-primary">
                            {order ? order.orderNo : '--'}
                          </span>
                          <span className="mt-1 text-xs font-bold uppercase text-slate-400">
                            {order ? order.subject || `tenant ${order.tenantId}` : '加载中'}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="px-8 py-6">
                      <div className="flex flex-col">
                        <span className="text-sm font-black text-slate-900">
                          {order ? `平台用户 ${order.platformUserId}` : '--'}
                        </span>
                        <span className="mt-0.5 text-[10px] font-bold text-slate-400">
                          {order ? order.createTime || '--' : '--'}
                        </span>
                      </div>
                    </td>
                    <td className="px-8 py-6 text-base font-black tracking-tight text-slate-900">
                      {order ? formatCurrency(order.totalAmount) : '...'}
                    </td>
                    <td className="px-8 py-6">
                      <div className="flex justify-center">
                        <span
                          className={cn(
                            'flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-[10px] font-black uppercase tracking-widest shadow-sm',
                            getOrderToneClass(lifecycle.tone),
                          )}
                        >
                          {order && order.orderStatus === 'CREATED' && <Clock size={10} />}
                          {order && order.payStatus === 'SUCCESS' && <Truck size={10} />}
                          {order && order.orderStatus === 'CLOSED' && <CheckCircle2 size={10} />}
                          {order && order.orderStatus === 'CANCELLED' && <XCircle size={10} />}
                          {lifecycle.label}
                        </span>
                      </div>
                    </td>
                    <td className="px-8 py-6 text-right">
                      {order && (
                        <div className="flex justify-end gap-2 opacity-0 transition-opacity group-hover:opacity-100">
                          <button
                            onClick={(event) => {
                              event.stopPropagation();
                              navigate(`/merchant/order/${order.orderNo}`);
                            }}
                            className="rounded-xl border border-slate-100 bg-slate-50 p-2 text-slate-400 transition-all hover:text-slate-900"
                          >
                            <Eye size={18} />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
