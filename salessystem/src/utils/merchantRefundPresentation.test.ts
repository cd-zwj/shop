import { describe, expect, it } from 'vitest';
import {
  getMerchantRefundPresentation,
  getMerchantRefundRiskItems,
} from './merchantRefundPresentation';
import type { Refund } from '../types/refund';

const baseRefund: Refund = {
  id: 1,
  refundNo: 'RA20260706001',
  orderNo: 'SO20260706001',
  orderItemId: 10,
  refundType: 'REFUND_ONLY',
  refundStatus: 'PENDING',
  refundAmount: 1999,
  deliveryStatus: 'PENDING',
  refundableAmount: 1999,
  quickRefundSuggested: true,
  refundSuggestion: null,
  reason: '不想要了',
  description: null,
  rejectReason: null,
  auditTime: null,
  completeTime: null,
  createTime: '2026-07-06T10:00:00',
};

describe('merchantRefundPresentation', () => {
  it('shows quick refund guidance for pending undelivered refunds', () => {
    expect(getMerchantRefundPresentation(baseRefund)).toEqual({
      statusLabel: '待审核',
      statusDescription: '用户已提交退款申请，当前交付未完成，可优先核对金额后快速处理。',
      nextAction: '建议：确认订单与可退金额无误后同意退款。',
      tone: 'warning',
      primaryActionLabel: '审核处理',
    });
  });

  it('warns merchant when delivered content must be revoked before refund', () => {
    const presentation = getMerchantRefundPresentation({
      ...baseRefund,
      quickRefundSuggested: false,
      deliveryStatus: 'DELIVERED',
      refundSuggestion: '同意后需先撤销交付再退款',
    });

    expect(presentation.statusDescription).toContain('同意后需先撤销交付再退款');
    expect(presentation.nextAction).toContain('先确认卡密、虚拟内容、服务核销或物流状态');
  });

  it('surfaces failed refund as manual follow-up', () => {
    expect(getMerchantRefundPresentation({
      ...baseRefund,
      refundStatus: 'FAILED',
      rejectReason: '交付撤销失败，请人工处理后再退款',
    })).toMatchObject({
      statusLabel: '退款失败',
      tone: 'danger',
      primaryActionLabel: '跟进失败原因',
      nextAction: '下一步：处理失败原因后重新推进退款，避免用户只看到停滞状态。',
    });
  });

  it('keeps rejection reason visible', () => {
    expect(getMerchantRefundPresentation({
      ...baseRefund,
      refundStatus: 'REJECTED',
      rejectReason: '商品已核销，无法退款',
    }).statusDescription).toBe('驳回原因：商品已核销，无法退款');
  });

  it('builds risk items from delivery and refundable amount', () => {
    expect(getMerchantRefundRiskItems({
      ...baseRefund,
      deliveryStatus: 'REVOKE_FAILED',
      refundableAmount: 500,
      refundAmount: 1999,
    })).toEqual([
      '交付撤销失败，需人工确认资源是否已回收。',
      '申请退款金额高于当前可退余额，请核对是否已有部分退款或抵扣。',
    ]);
  });
});
