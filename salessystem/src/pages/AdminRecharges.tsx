import { useEffect, useMemo, useState } from 'react';
import { Search, Wallet } from 'lucide-react';
import { adminTradeService } from '../services/modules/adminTrade';
import type { AdminRechargeOrder } from '../types/admin';
import { formatCurrency } from '../utils/display';

export default function AdminRecharges() {
  const [walletType, setWalletType] = useState('');
  const [bizStatus, setBizStatus] = useState('');
  const [tenantId, setTenantId] = useState('');
  const [orders, setOrders] = useState<AdminRechargeOrder[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadRecharges() {
      try {
        const result = await adminTradeService.listRechargeOrders({
          current: 1,
          size: 50,
          walletType: walletType || undefined,
          bizStatus: bizStatus || undefined,
          tenantId: tenantId ? Number(tenantId) : undefined,
        });
        if (!isMounted) return;
        setOrders(result.records ?? []);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('充值单列表加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadRecharges();

    return () => {
      isMounted = false;
    };
  }, [bizStatus, tenantId, walletType]);

  const totalRecharge = useMemo(
    () => orders.reduce((sum, item) => sum + Number(item.rechargeAmount || 0), 0),
    [orders],
  );
  const totalCredits = useMemo(
    () => orders.reduce((sum, item) => sum + Number(item.actualCreditAmount || 0), 0),
    [orders],
  );

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header>
        <h1 className="text-3xl font-black tracking-tight text-slate-900">充值订单监管</h1>
        <p className="mt-1 text-sm font-medium text-slate-500">
          当前页接入 `/v1/admin/recharge-orders`，展示真实充值金额、赠送金额、积分和实际到账金额。
        </p>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard label="当前列表充值单" value={String(orders.length)} />
        <SummaryCard label="充值总额" value={formatCurrency(totalRecharge)} />
        <SummaryCard label="实际到账总额" value={formatCurrency(totalCredits)} />
      </div>

      <div className="rounded-[24px] border border-slate-100 bg-white p-4 shadow-sm">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <select
            value={walletType}
            onChange={(event) => setWalletType(event.target.value)}
            className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm font-black text-slate-700 outline-none focus:border-primary focus:bg-white"
          >
            <option value="">全部钱包类型</option>
            <option value="UNIFIED">UNIFIED</option>
            <option value="MERCHANT">MERCHANT</option>
          </select>
          <select
            value={bizStatus}
            onChange={(event) => setBizStatus(event.target.value)}
            className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm font-black text-slate-700 outline-none focus:border-primary focus:bg-white"
          >
            <option value="">全部业务状态</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="WAIT_PAY">WAIT_PAY</option>
            <option value="PAYING">PAYING</option>
            <option value="FAILED">FAILED</option>
          </select>
          <div className="relative">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={tenantId}
              onChange={(event) => setTenantId(event.target.value)}
              placeholder="按 tenantId 筛选"
              className="w-full rounded-2xl border border-slate-100 bg-slate-50 py-3 pl-11 pr-4 text-sm font-medium outline-none focus:border-primary focus:bg-white"
            />
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-xl shadow-slate-100/40">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-slate-50/50">
              <tr>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">充值单号</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">主体信息</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">充值与赠送</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">到账结果</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {(isLoading ? Array.from({ length: 5 }) : orders).map((order, index) => {
                const isData = typeof order === 'object';
                return (
                  <tr key={isData ? order.rechargeNo : index} className="transition-colors hover:bg-slate-50/50">
                    <td className="px-8 py-6">
                      <p className="text-sm font-black text-slate-900">{isData ? order.rechargeNo : '加载中...'}</p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        {isData ? formatDateTime(order.createTime) : '--'}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <p className="text-sm font-medium text-slate-700">
                        walletType {isData ? order.walletType || '--' : '--'}
                      </p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        tenantId {isData ? order.tenantId ?? '--' : '--'} · userId {isData ? order.platformUserId ?? '--' : '--'}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <p className="text-sm font-black text-slate-900">
                        充值 {isData ? formatCurrency(order.rechargeAmount) : '--'}
                      </p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        送余额 {isData ? formatCurrency(order.giftAmount) : '--'} · 送积分 {isData ? order.giftPoints : '--'}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <p className="text-sm font-black text-primary">
                        {isData ? formatCurrency(order.actualCreditAmount) : '--'}
                      </p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        状态 {isData ? order.bizStatus || '--' : '--'}
                      </p>
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

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
      <div className="mb-4 w-fit rounded-2xl bg-slate-50 p-3">
        <Wallet className="h-5 w-5 text-primary" />
      </div>
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className="mt-2 text-2xl font-black tracking-tight text-slate-900">{value}</p>
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
