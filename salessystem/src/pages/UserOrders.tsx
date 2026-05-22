import { useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import {
  Ban,
  ChevronRight,
  CreditCard,
  RotateCcw,
  ShoppingBag,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { appOrderService } from '../services/modules/appOrder';
import { ApiError } from '../types/api';
import type { SalesOrder } from '../types/order';
import { cn } from '../lib/utils';
import { formatCurrency } from '../utils/display';
import { getPaymentBillReuseHint } from '../utils/paymentStatus';
import { canRepurchaseOrder } from '../utils/orderActions';

type OrderTabKey = 'all' | 'pending' | 'processing' | 'completed' | 'closed';

const ORDER_TABS: Array<{ key: OrderTabKey; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待支付' },
  { key: 'processing', label: '处理中' },
  { key: 'completed', label: '已完成' },
  { key: 'closed', label: '已关闭' },
];

function isPendingOrder(order: SalesOrder) {
  return order.payStatus === 'WAIT_PAY' || order.payStatus === 'PAYING';
}

function isCompletedOrder(order: SalesOrder) {
  return order.payStatus === 'SUCCESS' || order.orderStatus === 'PAID';
}

function isClosedOrder(order: SalesOrder) {
  return order.orderStatus === 'CANCELLED' || order.orderStatus === 'CLOSED' || order.payStatus === 'CLOSED';
}

function resolveOrderTab(order: SalesOrder): OrderTabKey {
  if (isPendingOrder(order)) {
    return 'pending';
  }

  if (isClosedOrder(order)) {
    return 'closed';
  }

  if (order.orderStatus === 'CLOSED') {
    return 'completed';
  }

  if (isCompletedOrder(order)) {
    return 'processing';
  }

  return 'all';
}

export default function UserOrders() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<SalesOrder[]>([]);
  const [paymentBillMap, setPaymentBillMap] = useState<Record<string, string>>({});
  const [selectedTab, setSelectedTab] = useState<OrderTabKey>('all');
  const [isLoading, setIsLoading] = useState(true);
  const [isActionLoading, setIsActionLoading] = useState<string | null>(null);
  const [actionHint, setActionHint] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadOrders() {
      try {
        const result = await appOrderService.listOrders(1, 20);
        if (!isMounted) return;
        const nextOrders = result.records ?? [];
        setOrders(nextOrders);

        const pendingOrders = nextOrders.filter((order) => isPendingOrder(order));
        if (pendingOrders.length > 0) {
          const details = await Promise.allSettled(
            pendingOrders.map((order) => appOrderService.getOrder(order.orderNo)),
          );

          if (!isMounted) return;

          const nextMap = details.reduce<Record<string, string>>((acc, detail, index) => {
            if (detail.status === 'fulfilled' && detail.value.paymentBillNo) {
              acc[pendingOrders[index].orderNo] = detail.value.paymentBillNo;
            }
            return acc;
          }, {});

          setPaymentBillMap(nextMap);
        } else {
          setPaymentBillMap({});
        }
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
  }, []);

  const filteredOrders = useMemo(() => {
    if (selectedTab === 'all') {
      return orders;
    }

    return orders.filter((order) => resolveOrderTab(order) === selectedTab);
  }, [orders, selectedTab]);

  async function handleCancelOrder(orderNo: string) {
    if (isActionLoading) {
      return;
    }

    setIsActionLoading(orderNo);
    setError('');
    setActionHint('');

    try {
      await appOrderService.cancelOrder(orderNo);
      const refreshed = await appOrderService.getOrder(orderNo);

      setOrders((currentOrders) =>
        currentOrders.map((order) => (order.orderNo === orderNo ? refreshed.order : order)),
      );
      setPaymentBillMap((currentMap) => {
        const nextMap = { ...currentMap };
        delete nextMap[orderNo];
        return nextMap;
      });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '取消订单失败，请稍后重试');
    } finally {
      setIsActionLoading(null);
    }
  }

  async function handleContinuePay(orderNo: string) {
    if (isActionLoading) {
      return;
    }

    setIsActionLoading(orderNo);
    setError('');
    setActionHint('');

    try {
      const payment = await appOrderService.repayOrder(orderNo, 'ALIPAY_PAGE');
      if (!payment.paymentBillNo) {
        throw new Error('当前订单未返回有效支付单号');
      }

      setPaymentBillMap((currentMap) => ({
        ...currentMap,
        [orderNo]: payment.paymentBillNo as string,
      }));
      setActionHint(getPaymentBillReuseHint(payment.reusedPaymentBill));

      if (payment.externalPayUrl) {
        window.open(payment.externalPayUrl, '_blank', 'noopener,noreferrer');
      }

      navigate(
        `/payment/status?billNo=${encodeURIComponent(payment.paymentBillNo)}&orderNo=${encodeURIComponent(orderNo)}&source=order&reused=${payment.reusedPaymentBill ? '1' : '0'}`,
      );
    } catch (err) {
      setError(
        err instanceof ApiError || err instanceof Error
          ? err.message
          : '拉起支付状态失败，请稍后重试',
      );
    } finally {
      setIsActionLoading(null);
    }
  }

  function renderStatus(order: SalesOrder) {
    if (isPendingOrder(order)) {
      return {
        label: '待支付',
        className: 'bg-orange-50 text-orange-600',
      };
    }

    if (order.orderStatus === 'CLOSED') {
      return {
        label: '已完成',
        className: 'bg-green-50 text-green-600',
      };
    }

    if (isClosedOrder(order)) {
      return {
        label: order.orderStatus === 'CANCELLED' ? '已取消' : '已关闭',
        className: 'bg-slate-100 text-slate-500',
      };
    }

    if (isCompletedOrder(order)) {
      return {
        label: '处理中',
        className: 'bg-blue-50 text-blue-600',
      };
    }

    return {
      label: `${order.orderStatus} / ${order.payStatus}`,
      className: 'bg-slate-100 text-slate-500',
    };
  }

  return (
    <div className="min-h-screen bg-slate-50 pb-32">
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-6 p-4 md:p-8">
        <header className="flex flex-col gap-2">
          <h1 className="text-3xl font-black tracking-tight text-slate-900">我的订单</h1>
          <p className="font-medium text-slate-500">
            这里展示后端真实订单列表，并支持继续支付、取消订单和查看详情。
          </p>
        </header>

        <div className="flex gap-2 overflow-x-auto py-2 hide-scrollbar">
          {ORDER_TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setSelectedTab(tab.key)}
              className={cn(
                'whitespace-nowrap rounded-2xl px-6 py-2.5 text-xs font-black uppercase tracking-widest transition-all',
                selectedTab === tab.key
                  ? 'bg-primary text-white shadow-lg shadow-primary/20'
                  : 'border border-slate-100 bg-white text-slate-400',
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

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

        <div className="flex flex-col gap-4">
          {(isLoading ? Array.from({ length: 3 }) : filteredOrders).map((order, index) => {
            const isData = typeof order === 'object';

            if (!isData) {
              return (
                <motion.div
                  key={index}
                  initial={{ opacity: 0, y: 10 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  className="rounded-[32px] border border-slate-100 bg-white p-6 shadow-sm"
                >
                  <div className="mb-4 h-5 w-36 rounded-full bg-slate-100" />
                  <div className="flex gap-4">
                    <div className="h-20 w-20 rounded-2xl border border-slate-100 bg-slate-50" />
                    <div className="flex flex-1 flex-col justify-between gap-4">
                      <div className="h-4 w-24 rounded-full bg-slate-100" />
                      <div className="h-8 w-32 rounded-full bg-slate-100" />
                      <div className="h-4 w-40 rounded-full bg-slate-100" />
                    </div>
                  </div>
                </motion.div>
              );
            }

            const status = renderStatus(order);
            const canCancel = isPendingOrder(order);
            const canContinuePay = isPendingOrder(order) && Boolean(paymentBillMap[order.orderNo] || order.externalPayAmount);
            const canRepurchase = canRepurchaseOrder(order);
            const isActing = isActionLoading === order.orderNo;

            return (
              <motion.div
                key={order.orderNo}
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                className="rounded-[32px] border border-slate-100 bg-white p-6 shadow-sm transition-all hover:shadow-xl hover:shadow-slate-200/50"
              >
                <div className="mb-4 flex items-center justify-between border-b border-slate-50 pb-4">
                  <div className="flex items-center gap-2">
                    <ShoppingBag className="h-4 w-4 text-primary" />
                    <span className="text-sm font-black text-slate-900">
                      订单号 {order.orderNo}
                    </span>
                  </div>
                  <span
                    className={cn(
                      'rounded-lg px-2.5 py-1 text-[10px] font-black uppercase tracking-wider',
                      status.className,
                    )}
                  >
                    {status.label}
                  </span>
                </div>

                <div className="flex gap-4">
                  <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl border border-slate-100 bg-slate-50">
                    <ShoppingBag className="h-8 w-8 text-slate-300" />
                  </div>
                  <div className="flex flex-1 flex-col justify-between">
                    <div>
                      <h3 className="text-[10px] font-black uppercase tracking-widest text-slate-400">
                        商户 ID / 订单主题
                      </h3>
                      <p className="mt-0.5 text-xl font-black text-slate-900">
                        {formatCurrency(order.totalAmount)}
                      </p>
                      <p className="mt-2 text-sm font-medium text-slate-500">
                        {order.subject || `tenant ${order.tenantId}`}
                      </p>
                    </div>
                    <div className="mt-4 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                      <span className="text-xs font-medium text-slate-400">
                        {`${order.createTime || '--'} · tenant ${order.tenantId}`}
                      </span>

                      <div className="flex flex-col items-start gap-2 lg:items-end">
                        {canContinuePay && (
                          <p className="text-xs font-medium text-slate-400">
                            继续支付时会优先复用仍有效的支付单，失效后再自动新建。
                          </p>
                        )}
                        <div className="flex flex-wrap gap-2">
                        {canContinuePay && (
                          <button
                            onClick={() => void handleContinuePay(order.orderNo)}
                            disabled={isActing}
                            className="flex items-center gap-2 rounded-2xl bg-primary px-4 py-2 text-xs font-black uppercase tracking-widest text-white shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            <CreditCard size={14} />
                            {isActing ? '处理中...' : '继续支付'}
                          </button>
                        )}
                        {canCancel && (
                          <button
                            onClick={() => void handleCancelOrder(order.orderNo)}
                            disabled={isActing}
                            className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-2 text-xs font-black uppercase tracking-widest text-slate-600 transition-all hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            <Ban size={14} />
                            {isActing ? '处理中...' : '取消订单'}
                          </button>
                        )}
                        {canRepurchase && (
                          <button
                            onClick={() => navigate(`/order/${order.orderNo}`)}
                            className="flex items-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-2 text-xs font-black uppercase tracking-widest text-emerald-700 transition-all hover:border-emerald-300 hover:bg-emerald-100"
                          >
                            <RotateCcw size={14} />
                            重新购买
                          </button>
                        )}
                        <button
                          onClick={() => navigate(`/order/${order.orderNo}`)}
                          className="flex items-center gap-2 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-2 text-xs font-black uppercase tracking-widest text-slate-500 transition-all hover:border-primary/20 hover:text-primary"
                        >
                          查看详情
                          <ChevronRight className="h-4 w-4" />
                        </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </motion.div>
            );
          })}
        </div>

        {!isLoading && filteredOrders.length === 0 && (
          <div className="flex flex-col items-center justify-center rounded-[32px] border border-slate-100 bg-white py-20 shadow-sm">
            <ShoppingBag className="mb-4 h-14 w-14 text-slate-200" />
            <p className="font-bold text-slate-500">当前筛选条件下没有订单</p>
          </div>
        )}
      </div>
    </div>
  );
}
