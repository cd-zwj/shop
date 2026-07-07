import { describe, expect, it } from 'vitest';
import type { MerchantOrder } from '../types/merchant';
import type { Refund } from '../types/refund';
import { summarizeMerchantOperations } from './merchantOperationsAnalytics';

function order(overrides: Partial<MerchantOrder>): MerchantOrder {
  return {
    id: overrides.id ?? Math.random(),
    orderNo: overrides.orderNo ?? `SO${Math.random()}`,
    tenantId: 9,
    platformUserId: overrides.platformUserId ?? 1,
    orderStatus: overrides.orderStatus ?? 'PAID',
    payStatus: overrides.payStatus ?? 'SUCCESS',
    totalAmount: overrides.totalAmount ?? 0,
    ...overrides,
  };
}

function refund(overrides: Partial<Refund>): Refund {
  return {
    id: overrides.id ?? Math.random(),
    refundNo: overrides.refundNo ?? `RF${Math.random()}`,
    orderNo: overrides.orderNo ?? 'SO1',
    orderItemId: null,
    refundType: 'REFUND_ONLY',
    refundStatus: overrides.refundStatus ?? 'COMPLETED',
    refundAmount: overrides.refundAmount ?? 0,
    deliveryStatus: null,
    refundableAmount: null,
    quickRefundSuggested: null,
    refundSuggestion: null,
    reason: '测试退款',
    description: null,
    rejectReason: null,
    auditTime: null,
    completeTime: null,
    createTime: '2026-07-01T00:00:00',
    ...overrides,
  };
}

describe('summarizeMerchantOperations', () => {
  it('summarizes average order value, refund rate, repeat customers and paid conversion', () => {
    const summary = summarizeMerchantOperations({
      orders: [
        order({ orderNo: 'SO1', platformUserId: 1, totalAmount: 100, payStatus: 'SUCCESS' }),
        order({ orderNo: 'SO2', platformUserId: 1, totalAmount: 80, payStatus: 'SUCCESS' }),
        order({ orderNo: 'SO3', platformUserId: 2, totalAmount: 50, payStatus: 'FAILED' }),
        order({ orderNo: 'SO4', platformUserId: 3, totalAmount: 120, payStatus: 'WAIT_PAY' }),
      ],
      refunds: [
        refund({ orderNo: 'SO1', refundStatus: 'COMPLETED', refundAmount: 30 }),
        refund({ orderNo: 'SO3', refundStatus: 'REJECTED', refundAmount: 20 }),
      ],
    });

    expect(summary).toEqual({
      orderCount: 4,
      paidOrderCount: 2,
      paidAmount: 180,
      averageOrderValue: 90,
      refundCaseCount: 1,
      refundAmount: 30,
      refundRate: 0.5,
      uniqueCustomerCount: 3,
      repeatCustomerCount: 1,
      repeatCustomerRate: 1 / 3,
      paidConversionRate: 0.5,
    });
  });

  it('ignores malformed amounts and handles empty data', () => {
    const summary = summarizeMerchantOperations({
      orders: [order({ orderNo: 'SO1', totalAmount: Number.NaN, payStatus: 'SUCCESS' })],
      refunds: [refund({ refundAmount: Number.POSITIVE_INFINITY, refundStatus: 'COMPLETED' })],
    });

    expect(summary.paidAmount).toBe(0);
    expect(summary.averageOrderValue).toBe(0);
    expect(summary.refundAmount).toBe(0);
    expect(summary.refundRate).toBe(1);

    expect(summarizeMerchantOperations({ orders: [], refunds: [] })).toEqual({
      orderCount: 0,
      paidOrderCount: 0,
      paidAmount: 0,
      averageOrderValue: 0,
      refundCaseCount: 0,
      refundAmount: 0,
      refundRate: 0,
      uniqueCustomerCount: 0,
      repeatCustomerCount: 0,
      repeatCustomerRate: 0,
      paidConversionRate: 0,
    });
  });
});
