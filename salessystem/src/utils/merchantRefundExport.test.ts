import { describe, expect, it } from 'vitest';
import { buildMerchantRefundsCsv } from './merchantRefundExport';
import type { Refund } from '../types/refund';

describe('merchantRefundExport', () => {
  it('builds a csv file for merchant refunds', () => {
    const csv = buildMerchantRefundsCsv([
      buildRefund({
        refundNo: 'RF202607070001',
        reason: '商品,不合适',
        refundStatus: 'FAILED',
        failureReason: '渠道退款失败',
      }),
    ]);

    expect(csv.startsWith('\ufeff')).toBe(true);
    expect(csv).toContain('"退款单号","订单号","订单项","退款类型","退款状态","退款金额","交付状态","失败原因","申请原因","创建时间"');
    expect(csv).toContain('"RF202607070001","SO202607070001","12","REFUND_ONLY","FAILED","28.50","PENDING","渠道退款失败","商品,不合适","2026-07-07T10:00:00"');
  });

  it('escapes dangerous spreadsheet formulas in text fields', () => {
    const csv = buildMerchantRefundsCsv([
      buildRefund({
        refundNo: '=cmd',
        reason: '+SUM(1,2)',
      }),
    ]);

    expect(csv).toContain('"\'=cmd"');
    expect(csv).toContain('"\'+SUM(1,2)"');
  });
});

function buildRefund(overrides: Partial<Refund>): Refund {
  return {
    id: 1,
    refundNo: 'RF202607070000',
    orderNo: 'SO202607070001',
    orderItemId: 12,
    refundType: 'REFUND_ONLY',
    refundStatus: 'PENDING',
    refundAmount: 28.5,
    deliveryStatus: 'PENDING',
    refundableAmount: 28.5,
    quickRefundSuggested: true,
    refundSuggestion: '建议快速退款',
    failureReason: null,
    availableActions: null,
    statusLabel: null,
    statusDescription: null,
    nextStep: null,
    reason: '不想要了',
    description: null,
    rejectReason: null,
    auditTime: null,
    completeTime: null,
    createTime: '2026-07-07T10:00:00',
    ...overrides,
  };
}
