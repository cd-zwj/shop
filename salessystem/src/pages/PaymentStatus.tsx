import { useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import {
  ArrowLeft,
  CheckCircle2,
  Clock,
  CreditCard,
  RefreshCcw,
  ShieldCheck,
  XCircle,
} from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { appPaymentBillService } from '../services/modules/appPaymentBill';
import type { PaymentBill } from '../types/payment';
import { cn } from '../lib/utils';
import { formatCurrency } from '../utils/display';

export default function PaymentStatus() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const billNo = searchParams.get('billNo');
  const source = searchParams.get('source');
  const [paymentBill, setPaymentBill] = useState<PaymentBill | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadBill(sync = false) {
      if (!billNo) {
        setError('缺少支付单号，无法查询支付状态');
        setIsLoading(false);
        return;
      }

      try {
        const bill = sync
          ? await appPaymentBillService.syncPaymentBill(billNo)
          : await appPaymentBillService.getPaymentBill(billNo);
        if (!isMounted) return;
        setPaymentBill(bill);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('支付状态查询失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
          setIsRefreshing(false);
        }
      }
    }

    void loadBill(false);

    let timer: number | undefined;
    timer = window.setInterval(() => {
      if (isMounted && billNo && paymentBill?.payStatus !== 'SUCCESS' && paymentBill?.payStatus !== 'FAILED' && paymentBill?.payStatus !== 'CLOSED') {
        void loadBill(true);
      }
    }, 10000);

    return () => {
      isMounted = false;
      if (timer) {
        window.clearInterval(timer);
      }
    };
  }, [billNo, paymentBill?.payStatus]);

  async function handleRefresh() {
    if (!billNo) {
      return;
    }
    setIsRefreshing(true);
    try {
      const bill = await appPaymentBillService.syncPaymentBill(billNo);
      setPaymentBill(bill);
      setError('');
    } catch {
      setError('刷新支付状态失败，请稍后重试');
    } finally {
      setIsRefreshing(false);
    }
  }

  const statusKey = useMemo(() => {
    const payStatus = paymentBill?.payStatus;
    if (payStatus === 'SUCCESS') return 'success';
    if (payStatus === 'FAILED' || payStatus === 'CLOSED') return 'failed';
    return 'pending';
  }, [paymentBill?.payStatus]);

  const content = {
    success: {
      icon: CheckCircle2,
      color: 'border-green-100 bg-green-50 text-green-500 shadow-green-200/50',
      title: '支付已确认',
      desc: source === 'recharge' ? '充值支付成功，资金会在钱包明细中体现。' : '订单支付成功，后续可进入订单详情继续查看。',
      primaryAction: source === 'recharge' ? '返回钱包' : '查看订单',
      primaryPath: source === 'recharge' ? '/wallet' : '/orders',
    },
    pending: {
      icon: Clock,
      color: 'border-orange-100 bg-orange-50 text-orange-500 shadow-orange-200/50',
      title: '交易确认中',
      desc: '后端支付单仍在同步状态中，页面会自动轮询，也可以手动刷新。',
      primaryAction: '刷新状态',
      primaryPath: null,
    },
    failed: {
      icon: XCircle,
      color: 'border-red-100 bg-red-50 text-red-500 shadow-red-200/50',
      title: '交易已关闭',
      desc: '支付单状态为失败或关闭，请返回上一步重新发起支付。',
      primaryAction: source === 'recharge' ? '重新充值' : '返回订单',
      primaryPath: source === 'recharge' ? '/recharge' : '/orders',
    },
  }[statusKey];

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-white p-6 md:p-12">
      <div className="absolute right-0 top-0 -mr-40 -mt-40 h-[500px] w-[500px] rounded-full bg-primary/5 blur-[100px]" />
      <div className="absolute bottom-0 left-0 -mb-20 -ml-20 h-[400px] w-[400px] rounded-full bg-slate-100 blur-[80px]" />

      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="relative z-10 flex w-full max-w-md flex-col items-center text-center"
      >
        <div className={cn('mb-8 flex h-32 w-32 items-center justify-center rounded-[40px] border-4 shadow-2xl transition-all duration-700', content.color)}>
          <content.icon size={56} className={statusKey === 'pending' ? 'animate-pulse-subtle' : ''} />
        </div>

        <h1 className="mb-4 text-4xl font-black tracking-tighter text-slate-900">{content.title}</h1>
        <p className="mb-8 font-medium leading-relaxed text-slate-500">{content.desc}</p>

        {error && (
          <div className="mb-6 w-full rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-left text-sm font-medium text-red-600">
            {error}
          </div>
        )}

        <div className="mb-12 flex w-full flex-col gap-4 rounded-[32px] border border-slate-100 bg-slate-50 p-6">
          <div className="flex items-center justify-between px-2">
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">支付单号</span>
            <span className="font-mono text-xs font-black tracking-tight text-slate-900 underline decoration-primary/20">
              {billNo || '--'}
            </span>
          </div>
          <div className="flex items-center justify-between px-2">
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">支付渠道</span>
            <div className="flex items-center gap-2">
              <CreditCard size={12} className="text-slate-400" />
              <span className="text-xs font-black text-slate-900">{paymentBill?.channelCode || 'ALIPAY_PAGE'}</span>
            </div>
          </div>
          <div className="flex items-center justify-between px-2">
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">支付金额</span>
            <span className="text-xs font-black text-slate-900">{formatCurrency(paymentBill?.payAmount)}</span>
          </div>
          <div className="flex items-center justify-between px-2">
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">支付状态</span>
            <span className="text-xs font-black text-primary">{paymentBill?.payStatus || (isLoading ? 'LOADING' : '--')}</span>
          </div>
        </div>

        <div className="flex w-full flex-col gap-4">
          <button
            onClick={() => (content.primaryPath ? navigate(content.primaryPath) : handleRefresh())}
            disabled={isRefreshing}
            className="flex w-full items-center justify-center gap-3 rounded-[24px] bg-primary py-5 text-lg font-black text-white shadow-2xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-70"
          >
            {content.primaryAction}
            {statusKey === 'pending' ? <RefreshCcw size={20} className={cn(isRefreshing && 'animate-spin')} /> : <ShieldCheck size={20} />}
          </button>
          <button
            onClick={() => navigate('/')}
            className="flex w-full items-center justify-center gap-2 py-5 text-sm font-black uppercase tracking-widest text-slate-400 transition-colors hover:text-slate-900"
          >
            <ArrowLeft size={16} /> 返回首页
          </button>
        </div>
      </motion.div>
    </div>
  );
}
