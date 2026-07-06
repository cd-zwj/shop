import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, AlertCircle } from 'lucide-react';
import { appOrderService } from '../services/modules/appOrder';
import { appRefundService } from '../services/modules/appRefund';
import { useToast } from '../context/ToastContext';
import type { SalesOrderDetail } from '../types/order';
import type { Refund } from '../types/refund';
import { formatCurrency } from '../utils/display';
import {
  getRefundProgressPresentation,
  getRefundStatusLabel,
  getRefundToneClass,
  isRefundApplicationActive,
} from '../utils/refundProgress';

export default function ApplyRefund() {
  const { orderNo } = useParams<{ orderNo: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  
  const [orderDetail, setOrderDetail] = useState<SalesOrderDetail | null>(null);
  const [refunds, setRefunds] = useState<Refund[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  // Form states
  const [refundType, setRefundType] = useState<'REFUND_ONLY' | 'RETURN_REFUND'>('REFUND_ONLY');
  const [refundAmount, setRefundAmount] = useState<number>(0);
  const [reason, setReason] = useState('不想要了');
  const [description, setDescription] = useState('');

  const reasons = [
    '不想要了',
    '商品质量问题',
    '商品与描述不符',
    '收到商品损坏',
    '发错货/漏发',
    '其他',
  ];

  const loadData = useCallback(async () => {
    if (!orderNo) return;
    setIsLoading(true);
    try {
      const detail = await appOrderService.getOrder(orderNo);
      setOrderDetail(detail);
      setRefundAmount(detail.order.payableAmount ?? detail.order.totalAmount);

      // Load existing refunds
      const refundsResult = await appRefundService.listRefunds(detail.order.tenantId);
      const filtered = refundsResult.records.filter((r) => r.orderNo === orderNo);
      setRefunds(filtered);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载退款信息失败');
    } finally {
      setIsLoading(false);
    }
  }, [orderNo]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!orderDetail) return;

    const maxAmount = orderDetail.order.payableAmount ?? orderDetail.order.totalAmount;
    if (refundAmount <= 0) {
      showToast('退款金额必须大于 0', 'error');
      return;
    }
    if (refundAmount > maxAmount) {
      showToast(`退款金额不能超过订单实付金额 ¥${maxAmount}`, 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      await appRefundService.applyRefund(orderDetail.order.tenantId, {
        orderNo: orderDetail.order.orderNo,
        refundType,
        refundAmount,
        reason,
        description: description.trim() || undefined,
      });
      showToast('退款申请已提交，请等待商家审核', 'success');
      await loadData();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '提交退款申请失败', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancelRefund = async (refundId: number) => {
    if (!orderDetail) return;
    try {
      await appRefundService.cancelRefund(orderDetail.order.tenantId, refundId);
      showToast('已成功取消该退款申请', 'success');
      await loadData();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '取消退款失败', 'error');
    }
  };

  if (isLoading) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-3 text-slate-500">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary/30 border-t-primary" />
        <span className="text-sm font-medium">加载订单与售后数据...</span>
      </div>
    );
  }

  if (error || !orderDetail) {
    return (
      <div className="mx-auto max-w-md px-4 py-12 text-center">
        <AlertCircle className="mx-auto mb-4 h-12 w-12 text-red-500" />
        <h2 className="mb-2 text-xl font-bold text-slate-900">数据加载失败</h2>
        <p className="mb-6 text-sm text-slate-500">{error || '未查找到对应的订单信息'}</p>
        <button onClick={() => navigate(-1)} className="rounded-xl bg-slate-900 px-6 py-2.5 text-sm font-bold text-white">
          返回上一页
        </button>
      </div>
    );
  }

  const activeRefund = refunds.find(isRefundApplicationActive);
  const maxRefundAmount = orderDetail.order.payableAmount ?? orderDetail.order.totalAmount;

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 pb-20">
      <div className="mb-6 flex items-center gap-3">
        <button onClick={() => navigate(-1)} className="rounded-xl border border-slate-200 bg-white p-2 text-slate-600 transition-colors hover:bg-slate-50">
          <ArrowLeft size={18} />
        </button>
        <h1 className="text-2xl font-black tracking-tight text-slate-900">退款售后</h1>
      </div>

      <div className="flex flex-col gap-6">
        {/* Order Card */}
        <section className="rounded-3xl border border-slate-100 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-xs font-black uppercase tracking-widest text-slate-400">对应订单信息</h2>
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between text-sm">
              <span className="text-slate-500">订单编号</span>
              <span className="font-mono font-bold text-slate-800">{orderDetail.order.orderNo}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-slate-500">下单时间</span>
              <span className="font-medium text-slate-600">{orderDetail.order.createTime || '--'}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-slate-500">实付金额</span>
              <span className="text-lg font-black text-slate-900">{formatCurrency(maxRefundAmount)}</span>
            </div>

            <div className="mt-4 border-t border-slate-50 pt-4">
              <h3 className="mb-3 text-xs font-bold text-slate-700">退款商品明细</h3>
              <div className="flex flex-col gap-3">
                {orderDetail.items.map((item) => (
                  <div key={item.id} className="flex justify-between text-sm">
                    <span className="text-slate-600 line-clamp-1">{item.productName} <span className="text-slate-400 font-bold">x {item.quantity}</span></span>
                    <span className="font-bold text-slate-900">{formatCurrency(item.price * item.quantity)}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Existing Refund Status Section */}
        {refunds.length > 0 && (
          <section className="rounded-3xl border border-slate-100 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-xs font-black uppercase tracking-widest text-slate-400">退款售后历史</h2>
            <div className="flex flex-col gap-4">
              {refunds.map((refund) => {
                const progress = getRefundProgressPresentation(refund);

                return (
                  <div key={refund.id} className="flex flex-col gap-3 rounded-2xl border border-slate-100 p-4">
                    <div className="rounded-2xl border border-slate-100 bg-slate-50 p-4">
                      <div className="mb-2 flex items-center justify-between gap-3">
                        <span className="text-xs font-black uppercase tracking-widest text-slate-400">处理节点</span>
                        <span className={`rounded-lg border px-2.5 py-1 text-xs font-bold ${getRefundToneClass(progress.tone)}`}>
                          {progress.label}
                        </span>
                      </div>
                      <p className="text-xs font-medium leading-relaxed text-slate-600">{progress.description}</p>
                      <p className="mt-2 text-xs font-semibold leading-relaxed text-slate-500">{progress.nextStep}</p>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-mono text-slate-400">退款单: {refund.refundNo}</span>
                      <span className={`rounded-full border px-2.5 py-0.5 text-xs font-black uppercase tracking-wider ${getRefundToneClass(progress.tone)}`}>
                        {getRefundStatusLabel(refund.refundStatus)}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                    <span className="text-slate-500">退款金额</span>
                    <span className="font-bold text-slate-900">{formatCurrency(refund.refundAmount)}</span>
                    
                    <span className="text-slate-500">退款原因</span>
                    <span className="font-medium text-slate-700">{refund.reason}</span>
                    
                    {refund.description && (
                      <>
                        <span className="text-slate-500">退款描述</span>
                        <span className="font-medium text-slate-700">{refund.description}</span>
                      </>
                    )}

                    {refund.rejectReason && (
                      <>
                        <span className="text-red-500 font-bold">驳回原因</span>
                        <span className="font-bold text-red-600">{refund.rejectReason}</span>
                      </>
                    )}
                  </div>

                  {refund.refundStatus === 'PENDING' && (
                    <button
                      onClick={() => handleCancelRefund(refund.id)}
                      className="mt-3 w-fit rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-bold text-red-500 transition-colors hover:bg-red-50"
                    >
                      取消退款申请
                    </button>
                  )}
                </div>
                );
              })}
            </div>
          </section>
        )}

        {/* Apply Refund Form */}
        {!activeRefund && (
          <section className="rounded-3xl border border-slate-100 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-xs font-black uppercase tracking-widest text-slate-400">新建退款申请</h2>
            <form onSubmit={handleSubmit} className="flex flex-col gap-6">
              {/* Refund Type */}
              <div className="flex flex-col gap-2">
                <span className="text-sm font-bold text-slate-700">退款类型</span>
                <div className="grid grid-cols-2 gap-4">
                  {([
                    { type: 'REFUND_ONLY', label: '仅退款', desc: '未收到货或协商退款' },
                    { type: 'RETURN_REFUND', label: '退货退款', desc: '收到商品需要退回且退款' },
                  ] as const).map((item) => (
                    <label
                      key={item.type}
                      className={`relative flex cursor-pointer flex-col rounded-2xl border-2 p-4 transition-all ${
                        refundType === item.type ? 'border-primary bg-white ring-4 ring-primary/5' : 'border-slate-100 bg-slate-50/50 hover:border-slate-200'
                      }`}
                    >
                      <input
                        type="radio"
                        checked={refundType === item.type}
                        onChange={() => setRefundType(item.type)}
                        className="sr-only"
                      />
                      <span className="text-sm font-bold text-slate-900">{item.label}</span>
                      <span className="mt-0.5 text-xs text-slate-500">{item.desc}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Refund Amount */}
              <div className="flex flex-col gap-2">
                <label htmlFor="refundAmount" className="text-sm font-bold text-slate-700">
                  退款金额 (元)
                </label>
                <div className="relative flex items-center">
                  <span className="absolute left-3 text-sm font-bold text-slate-400">¥</span>
                  <input
                    id="refundAmount"
                    type="number"
                    step="0.01"
                    min="0.01"
                    max={maxRefundAmount}
                    value={refundAmount}
                    onChange={(e) => setRefundAmount(Number(e.target.value))}
                    className="w-full rounded-2xl border border-slate-200 bg-white py-3 pl-8 pr-4 text-sm font-bold text-slate-800 outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>
                <span className="text-xs font-medium text-slate-400">
                  最多可退款 ¥{maxRefundAmount.toFixed(2)} (支持部分退款)
                </span>
              </div>

              {/* Refund Reason */}
              <div className="flex flex-col gap-2">
                <label htmlFor="reason" className="text-sm font-bold text-slate-700">
                  退款原因
                </label>
                <select
                  id="reason"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="w-full rounded-2xl border border-slate-200 bg-white p-3 text-sm font-bold text-slate-800 outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                >
                  {reasons.map((r) => (
                    <option key={r} value={r}>
                      {r}
                    </option>
                  ))}
                </select>
              </div>

              {/* Refund Description */}
              <div className="flex flex-col gap-2">
                <label htmlFor="description" className="text-sm font-bold text-slate-700">
                  详细描述 (选填)
                </label>
                <textarea
                  id="description"
                  rows={4}
                  placeholder="请在此填写具体的退款原因及相关细节说明..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full rounded-2xl border border-slate-200 bg-white p-4 text-sm font-medium text-slate-800 outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                />
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="mt-2 w-full rounded-2xl bg-primary py-4 font-bold text-white shadow-xl shadow-primary/20 transition-all hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isSubmitting ? '正在提交退款申请...' : '提交退款申请'}
              </button>
            </form>
          </section>
        )}
      </div>
    </div>
  );
}
