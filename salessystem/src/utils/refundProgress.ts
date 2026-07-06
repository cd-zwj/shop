import type { Refund } from '../types/refund';

export type RefundProgressTone = 'orange' | 'blue' | 'green' | 'red' | 'slate';

export interface RefundProgressPresentation {
  label: string;
  description: string;
  nextStep: string;
  tone: RefundProgressTone;
}

type RefundProgressInput = Partial<Pick<
  Refund,
  'refundStatus' | 'rejectReason' | 'refundSuggestion' | 'deliveryStatus'
>>;

const ACTIVE_REFUND_STATUSES = new Set(['PENDING', 'APPROVED', 'PROCESSING', 'COMPLETED']);

const REFUND_STATUS_LABELS: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '审核通过',
  PROCESSING: '退款处理中',
  COMPLETED: '退款完成',
  FAILED: '退款失败',
  REJECTED: '已驳回',
  CANCELLED: '已取消',
};

export function isRefundApplicationActive(refund: RefundProgressInput) {
  return ACTIVE_REFUND_STATUSES.has(refund.refundStatus ?? '');
}

export function getRefundStatusLabel(status?: string | null) {
  if (!status) {
    return '未知状态';
  }

  return REFUND_STATUS_LABELS[status] ?? status;
}

export function getRefundProgressPresentation(refund: RefundProgressInput): RefundProgressPresentation {
  const status = refund.refundStatus;
  const suggestion = refund.refundSuggestion?.trim();

  if (status === 'PENDING') {
    return {
      label: '待商家审核',
      description: suggestion || '退款申请已提交，等待商家确认订单、交付和可退金额。',
      nextStep: '预计节点：商家审核后会进入退款处理或给出驳回原因。',
      tone: 'orange',
    };
  }

  if (status === 'APPROVED' || status === 'PROCESSING') {
    return {
      label: '退款处理中',
      description: suggestion || '商家已同意退款，系统正在处理内部退款单和交付回退。',
      nextStep: '预计节点：内部退款单完成后会更新为退款完成；失败时会显示失败原因。',
      tone: 'blue',
    };
  }

  if (status === 'COMPLETED') {
    return {
      label: '退款完成',
      description: '退款流程已完成，可在订单和资产明细中继续追溯。',
      nextStep: '后续无需操作，如金额未变化请联系商户核对本地账务记录。',
      tone: 'green',
    };
  }

  if (status === 'FAILED') {
    return {
      label: '退款失败',
      description: refund.rejectReason ? `失败原因：${refund.rejectReason}` : '内部退款处理失败，需要商家或平台重新处理。',
      nextStep: '建议联系商户处理，或等待商家重新发起内部退款流程。',
      tone: 'red',
    };
  }

  if (status === 'REJECTED') {
    return {
      label: '已驳回',
      description: refund.rejectReason ? `驳回原因：${refund.rejectReason}` : '商家已驳回本次退款申请。',
      nextStep: '如仍需售后，可补充原因后重新提交，或联系商户沟通。',
      tone: 'red',
    };
  }

  if (status === 'CANCELLED') {
    return {
      label: '已取消',
      description: '该退款申请已取消，订单可按当前状态继续处理。',
      nextStep: '如仍需退款，可重新提交售后申请。',
      tone: 'slate',
    };
  }

  return {
    label: status || '未知状态',
    description: '该退款申请处于非常规状态，请查看订单或联系商户确认。',
    nextStep: '建议联系商户核对售后处理进度。',
    tone: 'slate',
  };
}

export function getRefundToneClass(tone: RefundProgressTone) {
  const classes: Record<RefundProgressTone, string> = {
    orange: 'border-orange-100 bg-orange-50 text-orange-600',
    blue: 'border-blue-100 bg-blue-50 text-blue-600',
    green: 'border-green-100 bg-green-50 text-green-600',
    red: 'border-red-100 bg-red-50 text-red-600',
    slate: 'border-slate-200 bg-slate-100 text-slate-500',
  };
  return classes[tone];
}
