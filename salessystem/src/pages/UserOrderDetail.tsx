import { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  Ban,
  CheckCircle2,
  CreditCard,
  HelpCircle,
  MapPin,
  MessageCircle,
  Package,
  RotateCcw,
  Truck,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { appOrderService } from '../services/modules/appOrder';
import { appRefundService } from '../services/modules/appRefund';
import { ApiError } from '../types/api';
import type { SalesOrderDetail, SalesOrderItem } from '../types/order';
import type { Refund } from '../types/refund';
import { cn } from '../lib/utils';
import { formatCurrency } from '../utils/display';
import { openAlipayPaymentWindow, saveAlipayPaymentPayload } from '../utils/alipayPayment';
import { getPaymentBillReuseHint } from '../utils/paymentStatus';
import { getPaymentStatusPresentation } from '../utils/paymentStatus';
import {
  buildRepurchaseCartItems,
  canRepurchaseOrder,
} from '../utils/orderActions';
import {
  getOrderLifecyclePresentation,
  getOrderProgressPresentation,
  getOrderToneClass,
} from '../utils/orderLifecycle';
import { getOrderItemFulfillmentPresentation } from '../utils/orderFulfillment';

export default function UserOrderDetail() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { addCartItems } = useCart();
  const [detail, setDetail] = useState<SalesOrderDetail | null>(null);
  const [refunds, setRefunds] = useState<Refund[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCancelling, setIsCancelling] = useState(false);
  const [actionHint, setActionHint] = useState('');
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
        setError('');

        try {
          const refundsResult = await appRefundService.listRefunds(result.order.tenantId, undefined, 1, 100);
          if (!isMounted) return;
          setRefunds((refundsResult.records ?? []).filter((refund) => refund.orderNo === result.order.orderNo));
        } catch {
          if (!isMounted) return;
          setRefunds([]);
          setActionHint('订单已加载，售后状态暂时同步失败，可稍后刷新或进入售后页查看。');
        }
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
  const shippingAddressText = order
    ? [order.shippingProvince, order.shippingCity, order.shippingDistrict, order.shippingDetail].filter(Boolean).join('')
    : '';
  const lifecycleWithContext = getOrderLifecyclePresentation(order, {
    items: detail?.items,
    refunds,
    paymentBillStatus: detail?.paymentBillStatus,
    paymentBillStatusRemark: detail?.paymentBillStatusRemark,
  });
  const latestPaymentPresentation = getPaymentStatusPresentation(
    detail?.paymentBillStatus
      ? {
          payStatus: detail.paymentBillStatus,
          expireTime: detail.paymentBillExpireTime,
          statusRemark: detail.paymentBillStatusRemark,
        }
      : order
        ? {
            payStatus: order.payStatus,
            expireTime: order.expireTime,
            statusRemark: null,
          }
        : null,
  );
  const canRepurchase = canRepurchaseOrder(order);
  const canContinuePay = lifecycleWithContext.nextActions.some((action) => action.key === 'pay');
  const canApplyRefund = lifecycleWithContext.nextActions.some((action) => action.key === 'refund');
  const canContactMerchant = lifecycleWithContext.nextActions.some((action) => action.key === 'contact');
  const canCancel =
    Boolean(order?.orderNo) &&
    order?.orderStatus !== 'PAID' &&
    order?.orderStatus !== 'CANCELLED' &&
    order?.orderStatus !== 'CLOSED' &&
    order?.payStatus !== 'SUCCESS' &&
    order?.payStatus !== 'CLOSED';

  const progress = getOrderProgressPresentation(order, lifecycleWithContext);
  const steps = useMemo(
    () => {
      const icons = [CreditCard, Package, Truck, CheckCircle2];
      return progress.steps.map((step, index) => ({
        ...step,
        icon: icons[index],
      }));
    },
    [progress.steps],
  );

  function handleRepurchase() {
    const nextItems = buildRepurchaseCartItems(detail);
    if (nextItems.length === 0) {
      setError('当前订单缺少可重新购买的商品明细');
      return;
    }

    setError('');
    setActionHint('已将原订单商品重新加入购物车，你可以重新确认后再结算。');
    addCartItems(nextItems);
    navigate('/cart');
  }

  async function handleCancelOrder() {
    if (!order?.orderNo || isCancelling) {
      return;
    }

    setIsCancelling(true);
    setError('');
    setActionHint('');

    try {
      await appOrderService.cancelOrder(order.orderNo);
      const refreshed = await appOrderService.getOrder(order.orderNo);
      setDetail(refreshed);
      void appRefundService.listRefunds(refreshed.order.tenantId, undefined, 1, 100)
        .then((refundsResult) => {
          setRefunds((refundsResult.records ?? []).filter((refund) => refund.orderNo === refreshed.order.orderNo));
        })
        .catch(() => {
          setActionHint('订单已更新，售后状态暂时同步失败，可稍后刷新或进入售后页查看。');
        });
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : '取消订单失败，请稍后重试',
      );
    } finally {
      setIsCancelling(false);
    }
  }

  async function handleContinuePay() {
    if (!order?.orderNo || isCancelling) {
      return;
    }

    setIsCancelling(true);
    setError('');
    setActionHint('');

    try {
      const payment = await appOrderService.repayOrder(order.orderNo, 'ALIPAY_PAGE');
      if (!payment.paymentBillNo) {
        throw new Error('当前订单未返回有效支付单号');
      }
      setActionHint(getPaymentBillReuseHint(payment.reusedPaymentBill));

      if (payment.externalPayUrl) {
        const isOpened = openAlipayPaymentWindow(payment.externalPayUrl);
        if (!isOpened && payment.paymentBillNo) {
          saveAlipayPaymentPayload({
            billNo: payment.paymentBillNo,
            orderNo: payment.orderNo ?? order?.orderNo,
            source: 'order',
            payHtml: payment.externalPayUrl,
            amount: payment.totalAmount,
          });
        }
      }

      navigate(
        `/payment/status?billNo=${encodeURIComponent(payment.paymentBillNo)}&orderNo=${encodeURIComponent(order.orderNo)}&source=order&reused=${payment.reusedPaymentBill ? '1' : '0'}`,
      );
    } catch (err) {
      setError(
        err instanceof ApiError || err instanceof Error
          ? err.message
          : '继续支付失败，请稍后重试',
      );
    } finally {
      setIsCancelling(false);
    }
  }

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
            <span className={cn('rounded-lg border px-3 py-1 text-xs font-bold', getOrderToneClass(lifecycleWithContext.tone))}>
              {lifecycleWithContext.label}
            </span>
          </div>
          <div className="relative flex justify-between">
            <div className="absolute left-0 right-0 top-5 z-0 h-0.5 bg-slate-100" />
            <div
              className="absolute left-0 top-5 z-0 h-0.5 bg-primary transition-all"
              style={{ width: `${progress.progressPercent}%` }}
            />
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
          <p className="mt-8 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-600">
            {lifecycleWithContext.description}
          </p>
          <p className="mt-3 rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm font-semibold text-blue-700">
            {lifecycleWithContext.nextStep}
          </p>
          {lifecycleWithContext.failureReason && (
            <p className="mt-3 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-semibold text-red-600">
              失败原因：{lifecycleWithContext.failureReason}
            </p>
          )}
        </section>

        {error && (
          <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
            {error}
          </div>
        )}
        {actionHint && !error && (
          <div className="rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
            {actionHint}
          </div>
        )}

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="flex flex-col gap-6 lg:col-span-2">
            <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
              <h3 className="mb-6 text-sm font-black uppercase tracking-widest text-slate-400">商品清单</h3>
              <div className="flex flex-col gap-6">
                {(isLoading ? Array.from<SalesOrderItem | undefined>({ length: 2 }) : detail?.items || []).map((item, index) => {
                  const fulfillment = item ? getOrderItemFulfillmentPresentation(item) : null;
                  return (
                    <div key={item ? item.id : index} className="group flex gap-4">
                      <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-slate-100 bg-slate-50">
                        <Package className="h-8 w-8 text-slate-300" />
                      </div>
                      <div className="flex flex-1 flex-col justify-center">
                        <div className="flex flex-wrap items-start justify-between gap-2">
                          <h4 className="font-black leading-tight text-slate-900">
                            {item ? item.productName : '加载商品中...'}
                          </h4>
                          {fulfillment && (
                            <span className={cn(
                              'rounded-lg border px-2 py-0.5 text-[10px] font-black',
                              getFulfillmentToneClass(fulfillment.tone),
                            )}>
                              {fulfillment.label}
                            </span>
                          )}
                        </div>
                        <div className="mt-2 flex items-center justify-between">
                          <span className="text-sm font-black text-primary">
                            {item ? formatCurrency(item.subtotal) : '...'}
                          </span>
                          <span className="text-xs font-bold text-slate-400">x {item ? item.quantity : '--'}</span>
                        </div>
                        {fulfillment && (
                          <div className="mt-3 rounded-2xl border border-slate-100 bg-slate-50 px-3 py-2">
                            <p className="text-xs font-medium leading-relaxed text-slate-600">
                              {fulfillment.description}
                            </p>
                            {fulfillment.actionPath && (
                              <button
                                type="button"
                                onClick={() => navigate(fulfillment.actionPath!)}
                                className="mt-2 text-xs font-black text-primary hover:text-primary/80"
                              >
                                {fulfillment.actionLabel}
                              </button>
                            )}
                          </div>
                        )}
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
                {(order?.shippingReceiverName || shippingAddressText) && (
                  <div className="mt-4 rounded-2xl border border-emerald-100 bg-emerald-50 p-4">
                    <div className="mb-2 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-emerald-700">
                      <MapPin className="h-4 w-4" /> 收货地址快照
                    </div>
                    <p className="text-sm font-bold text-slate-900">
                      {order.shippingReceiverName || '--'} {order.shippingPhone || ''}
                    </p>
                    <p className="mt-1 text-sm font-medium leading-relaxed text-slate-600">
                      {shippingAddressText || '该订单未记录详细地址'}
                    </p>
                    <p className="mt-2 text-xs font-medium text-emerald-700">
                      这是下单时锁定的地址，后续修改地址簿不会改变该订单。
                    </p>
                  </div>
                )}
                {detail?.paymentBillNo && (
                  <div className="mt-4 rounded-2xl border border-slate-100 bg-slate-50 p-4">
                    <div className="mb-2 flex items-center justify-between gap-3">
                      <span className="text-xs font-black uppercase tracking-widest text-slate-400">最近支付单</span>
                      <span className="font-mono text-xs font-black text-slate-700">{detail.paymentBillNo}</span>
                    </div>
                    <p className="text-sm font-bold text-slate-900">{latestPaymentPresentation.badgeLabel}</p>
                    <p className="mt-1 text-xs font-medium leading-relaxed text-slate-500">
                      {detail.paymentBillStatusRemark || latestPaymentPresentation.description}
                    </p>
                    {(latestPaymentPresentation.state === 'failed'
                      || latestPaymentPresentation.state === 'closed'
                      || latestPaymentPresentation.state === 'expired') && (
                      <p className="mt-2 rounded-xl border border-red-100 bg-red-50 px-3 py-2 text-xs font-semibold leading-relaxed text-red-600">
                        {latestPaymentPresentation.nextStep}
                      </p>
                    )}
                  </div>
                )}
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
                onClick={handleContinuePay}
                disabled={!order?.orderNo || isCancelling || !canContinuePay}
                className="relative z-10 mt-8 w-full rounded-2xl bg-white py-4 text-sm font-black text-slate-900 transition-all hover:bg-primary-container disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isCancelling ? '处理中...' : canContinuePay ? '继续支付 / 查看支付状态' : lifecycleWithContext.label}
              </button>
              <p className="relative z-10 mt-3 text-xs font-medium leading-relaxed text-slate-300">
                继续支付时会先尝试复用仍有效的支付单，若支付单已关闭、失败或过期，则自动新建。
              </p>
              {canRepurchase && (
                <button
                  onClick={handleRepurchase}
                  disabled={isCancelling}
                  className="relative z-10 mt-3 flex w-full items-center justify-center gap-2 rounded-2xl border border-white/15 bg-white/10 py-4 text-sm font-black text-white transition-all hover:bg-white/15 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <RotateCcw size={16} /> 重新购买同款商品
                </button>
              )}
              {canCancel && (
                <button
                  onClick={handleCancelOrder}
                  disabled={isCancelling}
                  className="relative z-10 mt-3 flex w-full items-center justify-center gap-2 rounded-2xl border border-white/15 bg-white/10 py-4 text-sm font-black text-white transition-all hover:bg-white/15 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <Ban size={16} /> {isCancelling ? '取消中...' : '取消订单'}
                </button>
              )}
            </section>

            <div className="flex flex-col gap-4 rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
              {canRepurchase && (
                <button
                  onClick={handleRepurchase}
                  className="flex w-full items-center justify-center gap-2 rounded-2xl bg-primary py-4 text-xs font-black uppercase tracking-widest text-white transition-all hover:opacity-95"
                >
                  <RotateCcw size={16} /> 重新加入购物车
                </button>
              )}
              <button
                onClick={() => order?.tenantId && navigate(`/merchant-store/${order.tenantId}`)}
                disabled={!canContactMerchant || !order?.tenantId}
                className="flex w-full items-center justify-center gap-2 rounded-2xl border-2 border-slate-100 py-4 text-xs font-black uppercase tracking-widest text-slate-600 transition-all hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-40"
              >
                <MessageCircle size={16} /> 联系商户
              </button>
              <button
                onClick={() => order?.orderNo && navigate(`/orders/${order.orderNo}/refund`)}
                disabled={!canApplyRefund || !order?.orderNo}
                className="flex w-full items-center justify-center gap-2 py-4 text-[10px] font-black uppercase tracking-widest text-slate-400 transition-all hover:text-slate-600 disabled:cursor-not-allowed disabled:opacity-40"
              >
                <HelpCircle size={14} /> 申请售后
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function getFulfillmentToneClass(tone: 'neutral' | 'warning' | 'success' | 'danger') {
  if (tone === 'success') return 'border-emerald-200 bg-emerald-50 text-emerald-700';
  if (tone === 'warning') return 'border-amber-200 bg-amber-50 text-amber-700';
  if (tone === 'danger') return 'border-red-200 bg-red-50 text-red-700';
  return 'border-slate-200 bg-slate-50 text-slate-600';
}
