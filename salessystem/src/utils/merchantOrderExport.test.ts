import { describe, expect, it } from 'vitest';
import { buildMerchantOrdersCsv } from './merchantOrderExport';
import type { MerchantOrder } from '../types/merchant';

describe('merchantOrderExport', () => {
  it('builds a csv file for merchant orders', () => {
    const csv = buildMerchantOrdersCsv([
      buildOrder({
        orderNo: 'MO202607070001',
        subject: '会员,订单',
        totalAmount: 128.5,
        payStatus: 'SUCCESS',
      }),
    ]);

    expect(csv.startsWith('\ufeff')).toBe(true);
    expect(csv).toContain('"订单号","订单主题","用户ID","订单状态","支付状态","订单金额","来源","创建时间"');
    expect(csv).toContain('"MO202607070001","会员,订单","88","PAID","SUCCESS","128.50","APP","2026-07-07T10:00:00"');
  });

  it('escapes dangerous spreadsheet formulas in text fields', () => {
    const csv = buildMerchantOrdersCsv([
      buildOrder({
        orderNo: '=cmd',
        subject: '+SUM(1,2)',
      }),
    ]);

    expect(csv).toContain('"\'=cmd"');
    expect(csv).toContain('"\'+SUM(1,2)"');
  });
});

function buildOrder(overrides: Partial<MerchantOrder>): MerchantOrder {
  return {
    id: 1,
    orderNo: 'MO202607070000',
    tenantId: 9,
    platformUserId: 88,
    orderStatus: 'PAID',
    payStatus: 'SUCCESS',
    totalAmount: 128,
    subject: '订单',
    source: 'APP',
    createTime: '2026-07-07T10:00:00',
    updateTime: null,
    ...overrides,
  };
}
