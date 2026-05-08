import { useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import {
  ArrowLeft,
  CheckCircle2,
  CreditCard,
  HelpCircle,
  MapPin,
  MessageCircle,
  Package,
  Truck,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { appOrderService } from '../services/modules/appOrder';
import type { SalesOrderDetail } from '../types/order';
import { cn } from '../lib/utils';
import { formatCurrency } from '../utils/display';

export default function UserOrderDetail() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [detail, setDetail] = useState<SalesOrderDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadOrderDetail() {
      if (!id) {
        setError('订单号缺失');
        setIsLoading(false);
        return;
      }

      try {
        const result = await appOrderService.getOrder(id);
        if (!isMounted) return;
        setDetail(result);
      } catch {
        if (!isMounted) return;
        setError('订单详情加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadOrderDetail();
    return () => {
      isMounted = false;
    };
  }, [id]);

  const order = detail?.order;
  const steps = useMemo(
    () => [
      { icon: CreditCard, label: '已创建', active: Boolean(order) },
      { icon: Package, label: '已支付', active: order?.payStatus === 'SUCCESS' || order?.orderStatus === 'PAID' },
      { icon: Truck, label: '处理中', active: order?.orderStatus === 'PAID' || order?.orderStatus === 'CREATED' },
      { icon: CheckCircle2, label: '已完成', active: order?.orderStatus === 'CLOSED' },
    ],
    [order],
  );

  return (
    <div className="min-h-screen bg-slate-50 pb-32">
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 p-4 md:p-8">
        <header className="flex items-center gap-4">
          <button onClick={() => navigate(-1)} className="rounded-2xl border border-slate-200 bg-white p-3 transition-all hover:bg-slate-50">
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-black tracking-tight text-slate-900">订单详情</h1>
            <p className="font-mono text-xs font-black uppercase tracking-widest text-slate-400">
              #{id || 'ORDER'}
            </p>
          </div>
        </header>

        <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
          <div className="mb-8 flex items-center justify-between">
            <span className="text-sm font-black uppercase tracking-widest text-slate-900">订单进度</span>
            <span className="rounded-lg bg-primary/5 px-3 py-1 text-xs font-bold text-primary">
              {order ? `${order.orderStatus} / ${order.payStatus}` : '加载中'}
            </span>
          </div>
          <div className="relative flex justify-between">
            <div className="absolute left-0 right-0 top-5 z-0 h-0.5 bg-slate-100" />
            <div className="absolute left-0 top-5 z-0 h-0.5 w-2/3 bg-primary" />
            {steps.map((step) => (
              <div key={step.label} className="relative z-10 flex flex-col items-center gap-3">
                <div
                  className={cn(
                    'flex h-10 w-10 items-center justify-center rounded-full border-4 border-white shadow-md transition-all',
                    step.active ? 'scale-110 bg-primary text-white' : 'bg-slate-100 text-slate-300',
                  )}
                >
                  <step.icon size={16} />
                </div>
                <span className={cn('text-[10px] font-black uppercase tracking-widest', step.active ? 'text-primary' : 'text-slate-300')}>
                  {step.label}
                </span>
              </div>
            ))}
          </div>
        </section>

        {error && (
          <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="flex flex-col gap-6 lg:col-span-2">
            <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
              <h3 className="mb-6 text-sm font-black uppercase tracking-widest text-slate-400">商品清单</h3>
              <div className="flex flex-col gap-6">
                {(isLoading ? Array.from({ length: 2 }) : detail?.items || []).map((item, index) => {
                  const isData = typeof item === 'object';
                  return (
                    <div key={isData ? item.id : index} className="group flex gap-4">
                      <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-slate-100 bg-slate-50">
                        <Package className="h-8 w-8 text-slate-300" />
                      </div>
                      <div className="flex flex-1 flex-col justify-center">
                        <h4 className="font-black leading-tight text-slate-900">
                          {isData ? item.productName : '加载商品中...'}
                        </h4>
                        <div className="mt-2 flex items-center justify-between">
                          <span className="text-sm font-black text-primary">
                            {isData ? formatCurrency(item.subtotal) : '...'}
                          </span>
                          <span className="text-xs font-bold text-slate-400">x {isData ? item.quantity : '--'}</span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>

            <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
              <div className="mb-6 flex items-center gap-3">
                <MapPin className="h-5 w-5 text-primary" />
                <h3 className="text-sm font-black uppercase tracking-widest text-slate-900">订单信息</h3>
              </div>
              <div className="flex flex-col gap-2">
                <span className="text-lg font-black text-slate-900">订单号：{order?.orderNo || '--'}</span>
                <p className="font-medium text-slate-500">创建时间：{order?.createTime || '--'}</p>
                <p className="font-medium text-slate-500">订单来源：{order?.source || '--'}</p>
                <p className="font-medium text-slate-500">支付策略：{order?.walletStrategy || '--'}</p>
              </div>
            </section>
          </div>

          <div className="flex flex-col gap-6">
            <section className="relative overflow-hidden rounded-[40px] bg-slate-900 p-8 text-white shadow-2xl">
              <div className="absolute right-0 top-0 -mr-8 -mt-8 rotate-12 opacity-10">
                <CreditCard size={160} />
              </div>
              <h3 className="relative z-10 mb-6 text-sm font-black uppercase tracking-widest text-slate-400">费用概览</h3>
              <div className="relative z-10 space-y-4">
                <div className="flex justify-between text-xs font-bold uppercase tracking-widest text-slate-400">
                  <span>商品合计</span>
                  <span className="text-white">{formatCurrency(order?.totalAmount)}</span>
                </div>
                <div className="flex justify-between text-xs font-bold uppercase tracking-widest text-slate-400">
                  <span>钱包抵扣</span>
                  <span className="text-white">{formatCurrency(order?.walletDeductAmount)}</span>
                </div>
                <div className="flex justify-between border-t border-white/5 pt-4 text-xs font-bold uppercase tracking-widest text-slate-400">
                  <span className="text-primary-container">待支付金额</span>
                  <span className="text-2xl font-black text-primary-container">{formatCurrency(order?.payableAmount ?? order?.externalPayAmount)}</span>
                </div>
              </div>
              <button
                onClick={() => order?.orderNo && navigate(`/payment/status?billNo=&orderNo=${encodeURIComponent(order.orderNo)}`)}
                className="relative z-10 mt-8 w-full rounded-2xl bg-white py-4 text-sm font-black text-slate-900 transition-all hover:bg-primary-container"
              >
                查看支付状态
              </button>
            </section>

            <div className="flex flex-col gap-4 rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
              <button className="flex w-full items-center justify-center gap-2 rounded-2xl border-2 border-slate-100 py-4 text-xs font-black uppercase tracking-widest text-slate-600 transition-all hover:border-primary hover:text-primary">
                <MessageCircle size={16} /> 联系商户
              </button>
              <button className="flex w-full items-center justify-center gap-2 py-4 text-[10px] font-black uppercase tracking-widest text-slate-400 transition-all hover:text-slate-600">
                <HelpCircle size={14} /> 申请售后
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
