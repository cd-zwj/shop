import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { ArrowRight, Receipt, Search, ShoppingBag, Wallet } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { adminTradeService } from '../services/modules/adminTrade';
import type { AdminOrderListItem, AdminTradeOverview } from '../types/admin';
import { formatCurrency } from '../utils/display';

const EMPTY_OVERVIEW: AdminTradeOverview = {
  totalOrders: 0,
  paidOrders: 0,
  pendingOrders: 0,
  totalOrderAmount: 0,
  totalExternalPayAmount: 0,
  totalPaymentBills: 0,
  paidPaymentBills: 0,
  totalPaymentAmount: 0,
  totalRechargeOrders: 0,
  successRechargeOrders: 0,
  totalRechargeAmount: 0,
};

export default function AdminTransactions() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [orderStatus, setOrderStatus] = useState('');
  const [payStatus, setPayStatus] = useState('');
  const [overview, setOverview] = useState<AdminTradeOverview>(EMPTY_OVERVIEW);
  const [orders, setOrders] = useState<AdminOrderListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadData() {
      try {
        const [nextOverview, nextOrders] = await Promise.all([
          adminTradeService.getOverview(),
          adminTradeService.listOrders({
            current: 1,
            size: 50,
            orderNo: keyword.trim() || undefined,
            orderStatus: orderStatus || undefined,
            payStatus: payStatus || undefined,
          }),
        ]);

        if (!isMounted) return;
        setOverview(nextOverview);
        setOrders(nextOrders.records ?? []);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('交易与订单数据加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadData();

    return () => {
      isMounted = false;
    };
  }, [keyword, orderStatus, payStatus]);

  const paidRate = useMemo(() => {
    if (!overview.totalOrders) return 0;
    return (overview.paidOrders / overview.totalOrders) * 100;
  }, [overview.paidOrders, overview.totalOrders]);

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header>
        <h1 className="text-3xl font-black tracking-tight text-slate-900">交易与订单总览</h1>
        <p className="mt-1 text-sm font-medium text-slate-500">
          第 7 部分这里已经切到真实接口：交易概览来自 `/v1/admin/trades/overview`，订单列表来自 `/v1/admin/orders`。
        </p>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
        <SummaryCard label="订单总数" value={String(overview.totalOrders)} hint={`${overview.pendingOrders} 笔待支付`} />
        <SummaryCard label="订单总额" value={formatCurrency(overview.totalOrderAmount)} hint={`外部支付 ${formatCurrency(overview.totalExternalPayAmount)}`} />
        <SummaryCard label="支付单总数" value={String(overview.totalPaymentBills)} hint={`${overview.paidPaymentBills} 笔已支付`} />
        <SummaryCard label="支付成功率" value={`${paidRate.toFixed(1)}%`} hint={`充值单 ${overview.totalRechargeOrders} 笔`} />
      </div>

      <div className="rounded-[24px] border border-slate-100 bg-white p-4 shadow-sm">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
          <div className="relative md:col-span-2">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="按订单号搜索..."
              className="w-full rounded-2xl border border-slate-100 bg-slate-50 py-3 pl-11 pr-4 text-sm font-medium outline-none transition-all focus:border-primary focus:bg-white"
            />
          </div>
          <select
            value={orderStatus}
            onChange={(event) => setOrderStatus(event.target.value)}
            className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm font-black text-slate-700 outline-none focus:border-primary focus:bg-white"
          >
            <option value="">全部订单状态</option>
            <option value="CREATED">CREATED</option>
            <option value="CLOSED">CLOSED</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
          <select
            value={payStatus}
            onChange={(event) => setPayStatus(event.target.value)}
            className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm font-black text-slate-700 outline-none focus:border-primary focus:bg-white"
          >
            <option value="">全部支付状态</option>
            <option value="WAIT_PAY">WAIT_PAY</option>
            <option value="PAYING">PAYING</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="FAILED">FAILED</option>
          </select>
        </div>
      </div>

      <div className="overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-xl shadow-slate-100/40">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-slate-50/50">
              <tr>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">订单信息</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">主体关系</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">金额</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">状态</th>
                <th className="px-8 py-5"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {(isLoading ? Array.from({ length: 5 }) : orders).map((order, index) => {
                const isData = typeof order === 'object';
                return (
                  <tr
                    key={isData ? order.orderNo : index}
                    className="cursor-pointer transition-colors hover:bg-slate-50/50"
                    onClick={() => isData && navigate(`/admin/order/${order.orderNo}`)}
                  >
                    <td className="px-8 py-6">
                      <p className="text-sm font-black text-slate-900">
                        {isData ? order.orderNo : '加载中...'}
                      </p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        {isData ? order.subject || '--' : '--'}
                      </p>
                      <p className="mt-1 text-[11px] font-medium text-slate-400">
                        {isData ? formatDateTime(order.createTime) : '--'}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <p className="text-sm font-medium text-slate-700">
                        tenantId: {isData ? order.tenantId : '--'}
                      </p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        userId: {isData ? order.platformUserId : '--'}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <p className="text-sm font-black text-slate-900">
                        {isData ? formatCurrency(order.totalAmount) : '--'}
                      </p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        外部支付 {isData ? formatCurrency(order.externalPayAmount || 0) : '--'}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <div className="flex flex-col gap-2">
                        <span className="w-fit rounded-lg bg-slate-100 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-slate-700">
                          {isData ? order.orderStatus : '--'}
                        </span>
                        <span className="w-fit rounded-lg bg-primary/5 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary">
                          {isData ? order.payStatus : '--'}
                        </span>
                      </div>
                    </td>
                    <td className="px-8 py-6 text-right">
                      {isData && (
                        <button
                          onClick={(event) => {
                            event.stopPropagation();
                            navigate(`/admin/order/${order.orderNo}`);
                          }}
                          className="inline-flex items-center gap-1 rounded-xl border border-slate-200 px-4 py-2 text-xs font-black text-slate-600 transition-all hover:border-primary hover:text-primary"
                        >
                          查看 <ArrowRight className="h-3 w-3" />
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <QuickCard icon={<ShoppingBag className="h-5 w-5 text-primary" />} title="订单详情" desc="支持按订单号查看真实订单主体与商品项。" />
        <QuickCard icon={<Receipt className="h-5 w-5 text-primary" />} title="支付单" desc="支付单列表已切到独立真实接口页面继续核对渠道与回调状态。" />
        <QuickCard icon={<Wallet className="h-5 w-5 text-primary" />} title="充值单" desc="充值单会展示钱包类型、赠送金额、积分和实际到账金额。" />
      </div>
    </div>
  );
}

function SummaryCard({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <div className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className="mt-2 text-2xl font-black tracking-tight text-slate-900">{value}</p>
      <p className="mt-2 text-sm font-medium text-slate-500">{hint}</p>
    </div>
  );
}

function QuickCard({ icon, title, desc }: { icon: ReactNode; title: string; desc: string }) {
  return (
    <div className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
      <div className="mb-4 w-fit rounded-2xl bg-slate-50 p-3">{icon}</div>
      <p className="text-sm font-black text-slate-900">{title}</p>
      <p className="mt-2 text-sm font-medium text-slate-500">{desc}</p>
    </div>
  );
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
