import React, { useCallback, useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { ClipboardList, AlertCircle, Check, X, ShieldAlert } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { merchantRefundService } from '../../services/modules/merchantRefund';
import type { Refund } from '../../types/refund';
import { formatCurrency } from '../../utils/display';

export default function MerchantRefunds() {
  const { merchantSession } = useAuth();
  const { showToast } = useToast();
  const tenantId = merchantSession?.tenantId;

  const [refunds, setRefunds] = useState<Refund[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<string>('ALL'); // ALL, PENDING, APPROVED, REJECTED
  
  // Audit Modal States
  const [auditingRefund, setAuditingRefund] = useState<Refund | null>(null);
  const [auditApproved, setAuditApproved] = useState<boolean>(true);
  const [rejectReason, setRejectReason] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const tabs = [
    { id: 'ALL', label: '全部' },
    { id: 'PENDING', label: '待审核' },
    { id: 'APPROVED', label: '已通过' },
    { id: 'REJECTED', label: '已驳回' },
  ];

  const loadRefunds = useCallback(async () => {
    if (!tenantId) return;
    setIsLoading(true);
    try {
      const result = await merchantRefundService.listRefunds(
        tenantId,
        activeTab === 'ALL' ? undefined : activeTab,
        1,
        50
      );
      setRefunds(result.records || []);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '获取售后单列表失败', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [tenantId, activeTab, showToast]);

  useEffect(() => {
    void loadRefunds();
  }, [loadRefunds]);

  const handleAuditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tenantId || !auditingRefund) return;

    if (!auditApproved && !rejectReason.trim()) {
      showToast('请填写驳回原因', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      await merchantRefundService.auditRefund(
        tenantId,
        auditingRefund.id,
        auditApproved,
        auditApproved ? undefined : rejectReason.trim()
      );
      showToast(auditApproved ? '退款申请已通过审核' : '退款申请已被驳回', 'success');
      setAuditingRefund(null);
      setRejectReason('');
      await loadRefunds();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '审核失败，请稍后重试', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <span className="inline-flex items-center gap-1 rounded-full bg-yellow-50 px-2.5 py-1 text-xs font-black text-yellow-600 border border-yellow-100">待审核</span>;
      case 'APPROVED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-green-50 px-2.5 py-1 text-xs font-black text-green-600 border border-green-100">已同意</span>;
      case 'COMPLETED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-green-50 px-2.5 py-1 text-xs font-black text-green-600 border border-green-100">已退款</span>;
      case 'REJECTED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-red-50 px-2.5 py-1 text-xs font-black text-red-600 border border-red-100">已驳回</span>;
      default:
        return <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-black text-slate-500 border border-slate-200">已取消</span>;
    }
  };

  return (
    <div className="flex flex-col gap-6 p-6 pb-20">
      <header className="flex flex-col gap-2">
        <h1 className="text-3xl font-black tracking-tight text-slate-900">退款与售后服务</h1>
        <p className="text-sm font-medium text-slate-500">
          管理并处理店铺中的售后申请。对待审核的申请，请及时查验并处理。
        </p>
      </header>

      {/* Tabs */}
      <div className="flex border-b border-slate-100 overflow-x-auto hide-scrollbar">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`whitespace-nowrap px-6 pb-4 text-sm font-bold border-b-2 transition-all ${
              activeTab === tab.id
                ? 'border-primary text-primary font-black scale-[1.02]'
                : 'border-transparent text-slate-400 hover:text-slate-600'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="flex min-h-[40vh] flex-col items-center justify-center gap-2 text-slate-500">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary/30 border-t-primary" />
          <span className="text-sm font-semibold">加载数据中...</span>
        </div>
      ) : refunds.length === 0 ? (
        <div className="flex min-h-[40vh] flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white py-12 text-slate-400">
          <ClipboardList className="mb-4 h-12 w-12 text-slate-300" />
          <p className="font-bold">暂无售后申请数据</p>
        </div>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {refunds.map((refund) => (
            <motion.article
              key={refund.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="flex flex-col overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-sm transition-all hover:shadow-md"
            >
              <div className="flex items-center justify-between border-b border-slate-50 bg-slate-50/50 p-5">
                <span className="font-mono text-xs font-bold text-slate-400">单号: {refund.refundNo}</span>
                {getStatusBadge(refund.refundStatus)}
              </div>

              <div className="flex-1 p-5 space-y-4">
                <div className="flex justify-between items-baseline">
                  <span className="text-2xl font-black text-slate-900">{formatCurrency(refund.refundAmount)}</span>
                  <span className="text-xs font-semibold text-slate-400">{refund.refundType === 'REFUND_ONLY' ? '仅退款' : '退货退款'}</span>
                </div>

                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-400">订单号</span>
                    <span className="font-mono font-medium text-slate-700">{refund.orderNo}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">申请原因</span>
                    <span className="font-medium text-slate-800">{refund.reason}</span>
                  </div>
                  {refund.description && (
                    <div className="flex flex-col gap-1 border-t border-slate-50 pt-2">
                      <span className="text-slate-400">详细描述</span>
                      <p className="text-xs text-slate-600 line-clamp-2 bg-slate-50 p-2 rounded-lg">{refund.description}</p>
                    </div>
                  )}
                  {refund.rejectReason && (
                    <div className="flex flex-col gap-1 border-t border-red-50 pt-2 text-red-600">
                      <span className="font-bold">驳回原因</span>
                      <p className="text-xs font-semibold bg-red-50/50 p-2 rounded-lg">{refund.rejectReason}</p>
                    </div>
                  )}
                </div>
              </div>

              {refund.refundStatus === 'PENDING' && (
                <div className="border-t border-slate-50 p-4 bg-slate-50/30">
                  <button
                    onClick={() => {
                      setAuditingRefund(refund);
                      setAuditApproved(true);
                    }}
                    className="flex w-full items-center justify-center gap-2 rounded-xl bg-slate-900 py-3 text-sm font-bold text-white transition-opacity hover:opacity-90"
                  >
                    <ShieldAlert size={16} />
                    审核处理
                  </button>
                </div>
              )}
            </motion.article>
          ))}
        </div>
      )}

      {/* Audit Modal */}
      <AnimatePresence>
        {auditingRefund && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="w-full max-w-md overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-2xl"
            >
              <div className="border-b border-slate-100 bg-slate-50 px-6 py-4">
                <h3 className="text-lg font-black text-slate-900">售后申请审核</h3>
              </div>

              <form onSubmit={handleAuditSubmit} className="p-6 space-y-6">
                <div className="space-y-2 text-sm text-slate-500">
                  <p>售后单号：<span className="font-mono font-bold text-slate-800">{auditingRefund.refundNo}</span></p>
                  <p>申请退款金额：<span className="font-bold text-red-500">{formatCurrency(auditingRefund.refundAmount)}</span></p>
                  <p>退款原因：<span className="font-semibold text-slate-800">{auditingRefund.reason}</span></p>
                </div>

                {/* Audit Choice */}
                <div className="flex flex-col gap-2">
                  <span className="text-sm font-bold text-slate-700">审核决策</span>
                  <div className="grid grid-cols-2 gap-4">
                    <button
                      type="button"
                      onClick={() => setAuditApproved(true)}
                      className={`flex items-center justify-center gap-2 rounded-2xl border-2 py-3 text-sm font-bold transition-all ${
                        auditApproved ? 'border-green-500 bg-green-50/30 text-green-600 ring-4 ring-green-500/5' : 'border-slate-100 bg-slate-50/50'
                      }`}
                    >
                      <Check size={18} />
                      同意退款
                    </button>
                    <button
                      type="button"
                      onClick={() => setAuditApproved(false)}
                      className={`flex items-center justify-center gap-2 rounded-2xl border-2 py-3 text-sm font-bold transition-all ${
                        !auditApproved ? 'border-red-500 bg-red-50/30 text-red-600 ring-4 ring-red-500/5' : 'border-slate-100 bg-slate-50/50'
                      }`}
                    >
                      <X size={18} />
                      拒绝驳回
                    </button>
                  </div>
                </div>

                {/* Reject Reason input */}
                {!auditApproved && (
                  <div className="flex flex-col gap-2">
                    <label htmlFor="rejectReason" className="text-sm font-bold text-slate-700">
                      驳回原因
                    </label>
                    <textarea
                      id="rejectReason"
                      rows={3}
                      placeholder="请详细填写拒绝和驳回此退款申请的原因，用户可见..."
                      value={rejectReason}
                      onChange={(e) => setRejectReason(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 bg-white p-3 text-sm font-medium text-slate-800 outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500"
                      required
                    />
                  </div>
                )}

                <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-50">
                  <button
                    type="button"
                    onClick={() => setAuditingRefund(null)}
                    className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-bold text-slate-500 hover:bg-slate-50"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className={`rounded-xl px-5 py-2.5 text-sm font-bold text-white transition-opacity hover:opacity-90 ${
                      auditApproved ? 'bg-green-600' : 'bg-red-600'
                    }`}
                  >
                    {isSubmitting ? '提交中...' : '提交审核'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
