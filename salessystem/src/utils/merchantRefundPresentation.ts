import type { Refund } from '../types/refund';

export type MerchantRefundTone = 'warning' | 'info' | 'success' | 'danger' | 'neutral';

export interface MerchantRefundPresentation {
  statusLabel: string;
  statusDescription: string;
  nextAction: string;
  tone: MerchantRefundTone;
  primaryActionLabel: string;
}

type MerchantRefundInput = Pick<
  Refund,
  | 'refundStatus'
  | 'deliveryStatus'
  | 'refundSuggestion'
  | 'quickRefundSuggested'
  | 'rejectReason'
  | 'refundAmount'
  | 'refundableAmount'
>;

const deliveredStatuses = new Set(['DELIVERED', 'CONFIRMED']);

function hasDeliveredContent(refund: MerchantRefundInput) {
  return refund.deliveryStatus ? deliveredStatuses.has(refund.deliveryStatus) : false;
}

function getSuggestion(refund: MerchantRefundInput) {
  return refund.refundSuggestion?.trim() || '';
}

export function getMerchantRefundPresentation(refund: MerchantRefundInput): MerchantRefundPresentation {
  const suggestion = getSuggestion(refund);

  if (refund.refundStatus === 'PENDING') {
    if (refund.quickRefundSuggested && !hasDeliveredContent(refund)) {
      return {
        statusLabel: '待审核',
        statusDescription: '用户已提交退款申请，当前交付未完成，可优先核对金额后快速处理。',
        nextAction: '建议：确认订单与可退金额无误后同意退款。',
        tone: 'warning',
        primaryActionLabel: '审核处理',
      };
    }

    return {
      statusLabel: '待审核',
      statusDescription: suggestion || '用户已提交退款申请，请先核对订单、交付状态和可退余额。',
      nextAction: hasDeliveredContent(refund)
        ? '建议：先确认卡密、虚拟内容、服务核销或物流状态，再决定同意或驳回。'
        : '建议：核对可退金额和申请原因后处理，避免售后单长时间停留。',
      tone: 'warning',
      primaryActionLabel: '审核处理',
    };
  }

  if (refund.refundStatus === 'APPROVED') {
    return {
      statusLabel: '已同意',
      statusDescription: suggestion || '商家已同意退款，系统将继续处理退款和交付回退。',
      nextAction: '下一步：关注退款处理结果，若涉及交付撤销需确认资源已回收。',
      tone: 'success',
      primaryActionLabel: '查看处理进度',
    };
  }

  if (refund.refundStatus === 'PROCESSING') {
    return {
      statusLabel: '退款中',
      statusDescription: suggestion || '退款正在处理，系统会同步更新内部退款和交付状态。',
      nextAction: '下一步：等待处理完成；如果长时间未完成，请核对退款和交付任务。',
      tone: 'info',
      primaryActionLabel: '查看处理进度',
    };
  }

  if (refund.refundStatus === 'COMPLETED') {
    return {
      statusLabel: '已退款',
      statusDescription: '退款已完成，订单和资产流水可继续追溯。',
      nextAction: '下一步：无需继续处理，可在财务或订单明细中核对记录。',
      tone: 'success',
      primaryActionLabel: '查看记录',
    };
  }

  if (refund.refundStatus === 'FAILED') {
    return {
      statusLabel: '退款失败',
      statusDescription: refund.rejectReason ? `失败原因：${refund.rejectReason}` : '退款处理失败，需要人工跟进失败原因。',
      nextAction: '下一步：处理失败原因后重新推进退款，避免用户只看到停滞状态。',
      tone: 'danger',
      primaryActionLabel: '跟进失败原因',
    };
  }

  if (refund.refundStatus === 'REJECTED') {
    return {
      statusLabel: '已驳回',
      statusDescription: refund.rejectReason ? `驳回原因：${refund.rejectReason}` : '商家已驳回本次退款申请。',
      nextAction: '下一步：确保驳回原因清晰可理解，便于用户决定是否重新提交售后。',
      tone: 'danger',
      primaryActionLabel: '查看驳回原因',
    };
  }

  if (refund.refundStatus === 'CANCELLED') {
    return {
      statusLabel: '已取消',
      statusDescription: '该退款申请已取消，订单按当前状态继续处理。',
      nextAction: '下一步：无需处理；如用户仍需售后，可引导重新提交。',
      tone: 'neutral',
      primaryActionLabel: '查看记录',
    };
  }

  return {
    statusLabel: refund.refundStatus || '未知状态',
    statusDescription: '该售后单处于非常规状态，请核对订单、交付和退款任务。',
    nextAction: '下一步：人工确认当前状态，避免用户售后进度不明确。',
    tone: 'neutral',
    primaryActionLabel: '查看详情',
  };
}

export function getMerchantRefundRiskItems(refund: MerchantRefundInput): string[] {
  const risks: string[] = [];

  if (refund.deliveryStatus === 'REVOKE_FAILED') {
    risks.push('交付撤销失败，需人工确认资源是否已回收。');
  }

  if (
    refund.refundableAmount !== null
    && refund.refundableAmount !== undefined
    && refund.refundAmount > refund.refundableAmount
  ) {
    risks.push('申请退款金额高于当前可退余额，请核对是否已有部分退款或抵扣。');
  }

  return risks;
}

export function getMerchantRefundToneClass(tone: MerchantRefundTone) {
  const classes: Record<MerchantRefundTone, string> = {
    warning: 'border-yellow-100 bg-yellow-50 text-yellow-700',
    info: 'border-blue-100 bg-blue-50 text-blue-700',
    success: 'border-green-100 bg-green-50 text-green-700',
    danger: 'border-red-100 bg-red-50 text-red-700',
    neutral: 'border-slate-200 bg-slate-100 text-slate-600',
  };
  return classes[tone];
}
