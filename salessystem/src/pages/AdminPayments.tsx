import { useEffect, useMemo, useState } from 'react';
import { ArrowRight, CreditCard } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { adminTradeService } from '../services/modules/adminTrade';
import type { AdminPaymentBill } from '../types/admin';
import { formatCurrency } from '../utils/display';

export default function AdminPayments() {
  const navigate = useNavigate();
  const [bizType, setBizType] = useState('');
  const [payStatus, setPayStatus] = useState('');
  const [channelCode, setChannelCode] = useState('');
  const [payments, setPayments] = useState<AdminPaymentBill[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadPayments() {
      try {
        const result = await adminTradeService.listPaymentBills({
          current: 1,
          size: 50,
          bizType: bizType || undefined,
          payStatus: payStatus || undefined,
          channelCode: channelCode || undefined,
        });
        if (!isMounted) return;
        setPayments(result.records ?? []);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('支付单列表加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadPayments();

    return () => {
      isMounted = false;
    };
  }, [bizType, payStatus, channelCode]);

  const totalAmount = useMemo(
    () => payments.reduce((sum, item) => sum + Number(item.payAmount || 0), 0),
    [payments],
  );
  const successCount = useMemo(
    () => payments.filter((item) => item.payStatus === 'SUCCESS').length,
    [payments],
  );

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header>
        <h1 className="text-3xl font-black tracking-tight text-slate-900">支付单流水监控</h1>
        <p className="mt-1 text-sm font-medium text-slate-500">
          当前页接入 `/v1/admin/payment-bills`，用于核对支付渠道、回调状态和业务单号。
        </p>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard label="当前列表支付单" value={String(payments.length)} />
        <SummaryCard label="成功支付单" value={String(successCount)} />
        <SummaryCard label="当前列表总金额" value={formatCurrency(totalAmount)} />
      </div>

      <div className="rounded-[24px] border border-slate-100 bg-white p-4 shadow-sm">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <select
            value={bizType}
            onChange={(event) => setBizType(event.target.value)}
            className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm font-black text-slate-700 outline-none focus:border-primary focus:bg-white"
          >
            <option value="">全部业务类型</option>
            <option value="ORDER">ORDER</option>
            <option value="RECHARGE">RECHARGE</option>
          </select>
          <select
            value={payStatus}
            onChange={(event) => setPayStatus(event.target.value)}
            className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm font-black text-slate-700 outline-none focus:border-primary focus:bg-white"
          >
            <option value="">全部支付状态</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="WAIT_PAY">WAIT_PAY</option>
            <option value="PAYING">PAYING</option>
            <option value="FAILED">FAILED</option>
          </select>
          <input
            type="text"
            value={channelCode}
            onChange={(event) => setChannelCode(event.target.value)}
            placeholder="按渠道码筛选"
            className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm font-medium outline-none focus:border-primary focus:bg-white"
          />
        </div>
      </div>

      <div className="overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-xl shadow-slate-100/40">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-slate-50/50">
              <tr>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">支付单号</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">业务关联</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">金额</th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">状态</th>
                <th className="px-8 py-5"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {(isLoading ? Array.from({ length: 5 }) : payments).map((payment, index) => {
                const isData = typeof payment === 'object';
                return (
                  <tr key={isData ? payment.billNo : index} className="transition-colors hover:bg-slate-50/50">
                    <td className="px-8 py-6">
                      <p className="text-sm font-black text-slate-900">{isData ? payment.billNo : '加载中...'}</p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        {isData ? formatDateTime(payment.createTime) : '--'}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <p className="text-sm font-medium text-slate-700">
                        {isData ? `${payment.bizType || '--'} / ${payment.bizNo || '--'}` : '--'}
                      </p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        渠道 {isData ? payment.channelCode || '--' : '--'} · userId {isData ? payment.platformUserId ?? '--' : '--'}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <p className="text-sm font-black text-slate-900">
                        {isData ? formatCurrency(payment.payAmount) : '--'}
                      </p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        第三方单号 {isData ? payment.thirdPartyBillNo || '--' : '--'}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <div className="flex flex-col gap-2">
                        <span className="w-fit rounded-lg bg-primary/5 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary">
                          {isData ? payment.payStatus || '--' : '--'}
                        </span>
                        <span className="w-fit rounded-lg bg-slate-100 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-slate-700">
                          callback {isData ? payment.callbackStatus || '--' : '--'}
                        </span>
                      </div>
                    </td>
                    <td className="px-8 py-6 text-right">
                      {isData && payment.bizType === 'ORDER' && payment.bizNo && (
                        <button
                          onClick={() => navigate(`/admin/order/${payment.bizNo}`)}
                          className="inline-flex items-center gap-1 rounded-xl border border-slate-200 px-4 py-2 text-xs font-black text-slate-600 transition-all hover:border-primary hover:text-primary"
                        >
                          订单详情 <ArrowRight className="h-3 w-3" />
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
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
      <div className="mb-4 w-fit rounded-2xl bg-slate-50 p-3">
        <CreditCard className="h-5 w-5 text-primary" />
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
