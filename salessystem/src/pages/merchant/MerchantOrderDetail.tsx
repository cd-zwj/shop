import { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  ChevronRight,
  Clock,
  CreditCard,
  MapPin,
  MessageCircle,
  Package,
  Send,
  ShieldCheck,
  TicketCheck,
  Truck,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { merchantOrderService } from '../../services/modules/merchantOrder';
import type { MerchantOrderDetail } from '../../types/merchant';
import { cn } from '../../lib/utils';
import { formatCurrency } from '../../utils/display';

export default function MerchantOrderDetail() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { merchantSession } = useAuth();
  const { showToast } = useToast();
  const tenantId = merchantSession?.tenantId;
  const [detail, setDetail] = useState<MerchantOrderDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [shippingItemId, setShippingItemId] = useState<number | null>(null);
  const [shipForm, setShipForm] = useState({ shippingNo: '', logisticsCompany: '' });
  const [isShipping, setIsShipping] = useState(false);
  const [verifyCode, setVerifyCode] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);

  async function reload() {
    if (!tenantId || !id) return;
    try {
      const result = await merchantOrderService.getOrderDetail(tenantId, id);
      setDetail(result);
      setError('');
    } catch {
      setError('订单详情加载失败，请稍后重试');
    }
  }

  async function handleShip() {
    if (!tenantId || shippingItemId == null) return;
    if (!shipForm.shippingNo.trim()) {
      showToast('请填写物流单号', 'error');
      return;
    }
    setIsShipping(true);
    try {
      await merchantOrderService.shipItem(tenantId, shippingItemId, shipForm.shippingNo.trim(), shipForm.logisticsCompany.trim() || undefined);
      showToast('发货成功', 'success');
      setShippingItemId(null);
      setShipForm({ shippingNo: '', logisticsCompany: '' });
      await reload();
    } catch (err) {
      const msg = err instanceof Error ? err.message : '发货失败';
      showToast(msg, 'error');
    } finally {
      setIsShipping(false);
    }
  }

  async function handleVerifyService() {
    if (!tenantId) return;
    const code = verifyCode.trim();
    if (!code) {
      showToast('请填写核销码', 'error');
      return;
    }
    setIsVerifying(true);
    try {
      await merchantOrderService.verifyService(tenantId, code);
      showToast('核销成功', 'success');
      setVerifyCode('');
      await reload();
    } catch (err) {
      const msg = err instanceof Error ? err.message : '核销失败';
      showToast(msg, 'error');
    } finally {
      setIsVerifying(false);
    }
  }

  useEffect(() => {
    let isMounted = true;

    async function loadDetail() {
      if (!tenantId || !id) {
        setError('订单参数缺失，请重新进入页面');
        setIsLoading(false);
        return;
      }

      try {
        const result = await merchantOrderService.getOrderDetail(tenantId, id);
        if (!isMounted) return;
        setDetail(result);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('订单详情加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadDetail();
    return () => {
      isMounted = false;
    };
  }, [id, tenantId]);

  const order = detail?.order;
  const timeline = useMemo(
    () => [
      {
        time: order?.createTime || '--',
        label: '订单已创建并写入商户订单池',
        active: Boolean(order),
        icon: ShieldCheck,
      },
      {
        time: order?.updateTime || '--',
        label: `支付状态：${order?.payStatus || '--'}`,
        active: order?.payStatus === 'SUCCESS',
        icon: CreditCard,
      },
      {
        time: order?.updateTime || '--',
        label: `订单状态：${order?.orderStatus || '--'}`,
        active: order?.orderStatus === 'PAID' || order?.orderStatus === 'CLOSED',
        icon: Package,
      },
    ],
    [order],
  );

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 p-4 pb-32 md:p-8">
      <header className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end">
        <div>
          <button
            onClick={() => navigate(-1)}
            className="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-primary transition-all hover:gap-3"
          >
            <ArrowLeft size={16} /> 返回订单列表
          </button>
          <div className="flex items-center gap-4">
            <h1 className="text-4xl font-black tracking-tight text-slate-900">
              管理订单 <span className="font-mono text-slate-400">#{id || 'ORDER'}</span>
            </h1>
            {order && (
              <span className="rounded-lg border border-orange-100 bg-orange-50 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-orange-600">
                {order.orderStatus}/{order.payStatus}
              </span>
            )}
          </div>
          <p className="mt-1 font-medium text-slate-500">
            创建时间：{order?.createTime || '--'} · 当前商户：{merchantSession?.tenantName || '--'}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button className="flex items-center gap-2 rounded-[24px] bg-primary px-8 py-4 text-base font-black text-white shadow-2xl shadow-primary/20 transition-all hover:scale-105 active:scale-95">
            <Send size={20} /> 标记履约跟进
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <div className="flex flex-col gap-8 lg:col-span-8">
          <section className="rounded-[40px] border border-slate-100 bg-white p-10 shadow-sm">
            <div className="mb-8 flex items-center justify-between border-b border-slate-50 pb-8">
              <div className="flex items-center gap-4">
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-900 text-white">
                  <CreditCard size={28} />
                </div>
                <div>
                  <h4 className="text-[10px] font-black uppercase tracking-widest text-slate-400">
                    支付方式 / 状态
                  </h4>
                  <p className="text-lg font-black text-slate-900">
                    外部支付/钱包混合 · <span className="text-green-500">{order?.payStatus || '--'}</span>
                  </p>
                </div>
              </div>
              <div className="text-right">
                <h4 className="text-[10px] font-black uppercase tracking-widest text-slate-400">订单总金额</h4>
                <p className="text-3xl font-black text-slate-900">{formatCurrency(order?.totalAmount)}</p>
              </div>
            </div>

            <h3 className="mb-6 text-[10px] font-black uppercase tracking-widest text-slate-400">商品明细</h3>
            <div className="space-y-6">
              {(isLoading ? Array.from({ length: 1 }) : detail?.items || []).map((item: any, index: number) => {
                const isData = item && typeof item === 'object' && 'id' in item;
                const productType = isData ? (item.productType ?? 'PHYSICAL') : null;
                const deliveryStatus = isData ? (item.deliveryStatus ?? 'PENDING') : null;
                const canShip =
                  isData &&
                  productType === 'PHYSICAL' &&
                  order?.payStatus === 'SUCCESS' &&
                  deliveryStatus !== 'DELIVERED' &&
                  deliveryStatus !== 'CONFIRMED' &&
                  deliveryStatus !== 'REVOKED';
                return (
                  <div key={isData ? item.id : index} className="flex gap-6">
                    <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-slate-100">
                      <Package className="h-8 w-8 text-slate-300" />
                    </div>
                    <div className="flex flex-1 flex-col justify-center">
                      <div className="flex items-start justify-between">
                        <h4 className="text-lg font-black leading-tight text-slate-900">
                          {isData ? item.productName : '加载商品中...'}
                        </h4>
                        <span className="text-base font-black text-slate-900">
                          {isData ? formatCurrency(item.subtotal) : '...'}
                        </span>
                      </div>
                      <div className="mt-2 flex items-center justify-between">
                        <span className="text-xs font-bold uppercase tracking-widest text-slate-400">
                          SKU/Product ID: {isData ? item.productId : '--'}
                        </span>
                        <span className="text-xs font-black text-slate-900">x {isData ? item.quantity : '--'}</span>
                      </div>
                      {isData && (
                        <div className="mt-3 flex items-center justify-between gap-3">
                          <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest">
                            <span className="rounded-md bg-slate-100 px-2 py-0.5 text-slate-600">{productType}</span>
                            <span className="rounded-md bg-amber-50 px-2 py-0.5 text-amber-700">{deliveryStatus}</span>
                          </div>
                          {canShip && (
                            <button
                              onClick={() => setShippingItemId(item.id)}
                              className="flex items-center gap-1.5 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-black text-white hover:bg-slate-800"
                            >
                              <Truck className="h-3.5 w-3.5" /> 发货
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

          <section className="rounded-[40px] border border-slate-100 bg-white p-10 shadow-sm">
            <h3 className="mb-8 text-sm font-black uppercase tracking-widest text-slate-900">履约时间线</h3>
            <div className="relative space-y-8">
              <div className="absolute bottom-2 left-4 top-2 z-0 w-0.5 bg-slate-100" />
              {timeline.map((log) => (
                <div key={log.label} className="relative z-10 flex gap-6">
                  <div
                    className={cn(
                      'flex h-8 w-8 items-center justify-center rounded-full shadow-md',
                      log.active ? 'bg-primary text-white' : 'bg-slate-100 text-slate-300',
                    )}
                  >
                    <log.icon size={14} />
                  </div>
                  <div className="flex flex-col pt-1">
                    <span className="font-mono text-[10px] font-black text-primary">{log.time}</span>
                    <span className={cn('mt-0.5 text-sm font-bold', log.active ? 'text-slate-900' : 'text-slate-300')}>
                      {log.label}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>

        <div className="flex flex-col gap-8 lg:col-span-4">
          <section className="flex flex-col gap-8 rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
            <div>
              <h3 className="mb-6 text-[10px] font-black uppercase tracking-widest text-slate-500">买家身份信息</h3>
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10 text-lg font-black">
                  U
                </div>
                <div>
                  <p className="text-lg font-black">平台用户 UID: {order?.platformUserId || '--'}</p>
                  <span className="text-xs font-medium text-slate-500">当前接口未返回收货人与地址详情</span>
                </div>
              </div>
            </div>

            <div className="border-t border-white/5 pt-6">
              <label className="text-[10px] font-black uppercase tracking-widest text-slate-500">订单业务信息</label>
              <div className="mt-4 flex gap-3">
                <MapPin className="h-5 w-5 shrink-0 text-primary" />
                <div className="space-y-1 text-sm font-medium leading-relaxed text-slate-300">
                  <p>订单来源：{order?.source || '--'}</p>
                  <p>支付策略：{order?.walletStrategy || '--'}</p>
                  <p>到期时间：{order?.expireTime || '--'}</p>
                </div>
              </div>
            </div>

            <button className="flex w-full items-center justify-center gap-2 rounded-2xl border border-white/10 bg-white/5 py-4 text-xs font-black uppercase tracking-widest transition-all hover:bg-white/10">
              <MessageCircle size={14} /> 发起 IM 对话
            </button>
          </section>

          <section className="flex flex-col gap-6 rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
            <h3 className="text-[10px] font-black uppercase tracking-widest text-slate-400">操作面板</h3>
            <div className="flex flex-col gap-4">
              <div className="rounded-2xl bg-sky-50 p-4">
                <div className="mb-3 flex items-center gap-2 text-sm font-black text-sky-900">
                  <TicketCheck size={16} /> 服务核销
                </div>
                <div className="flex gap-2">
                  <input
                    type="text"
                    inputMode="numeric"
                    value={verifyCode}
                    onChange={(e) => setVerifyCode(e.target.value)}
                    className="min-w-0 flex-1 rounded-xl border border-sky-100 bg-white px-3 py-2 text-sm font-bold text-slate-900 outline-none focus:border-sky-400"
                    placeholder="输入核销码"
                  />
                  <button
                    onClick={() => void handleVerifyService()}
                    disabled={isVerifying}
                    className="shrink-0 rounded-xl bg-sky-600 px-4 py-2 text-xs font-black text-white hover:bg-sky-700 disabled:opacity-50"
                  >
                    {isVerifying ? '提交中' : '核销'}
                  </button>
                </div>
              </div>
              <div className="flex items-center justify-between rounded-2xl bg-slate-50 p-4">
                <span className="text-sm font-bold text-slate-600">订单状态</span>
                <ChevronRight size={14} className="text-slate-300" />
              </div>
              <div className="flex items-center justify-between rounded-2xl bg-slate-50 p-4">
                <span className="text-sm font-bold text-slate-600">支付状态</span>
                <ChevronRight size={14} className="text-slate-300" />
              </div>
            </div>
          </section>
        </div>
      </div>

      {shippingItemId != null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
          <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
            <h3 className="mb-4 text-lg font-black text-slate-900">填写发货信息</h3>
            <div className="flex flex-col gap-4">
              <label className="flex flex-col gap-2">
                <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">物流单号</span>
                <input
                  type="text"
                  value={shipForm.shippingNo}
                  onChange={(e) => setShipForm((p) => ({ ...p, shippingNo: e.target.value }))}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium outline-none focus:border-primary"
                  placeholder="如 SF1234567890"
                />
              </label>
              <label className="flex flex-col gap-2">
                <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">承运商(可选)</span>
                <input
                  type="text"
                  value={shipForm.logisticsCompany}
                  onChange={(e) => setShipForm((p) => ({ ...p, logisticsCompany: e.target.value }))}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium outline-none focus:border-primary"
                  placeholder="顺丰 / 圆通 / 中通 ..."
                />
              </label>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={() => {
                  setShippingItemId(null);
                  setShipForm({ shippingNo: '', logisticsCompany: '' });
                }}
                className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-50"
              >
                取消
              </button>
              <button
                onClick={() => void handleShip()}
                disabled={isShipping}
                className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-black text-white hover:bg-slate-800 disabled:opacity-50"
              >
                {isShipping ? '提交中...' : '确认发货'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
