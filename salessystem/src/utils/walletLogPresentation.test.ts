import { describe, expect, it } from 'vitest';
import type { WalletLog } from '../types/wallet';
import { buildWalletRecentEntries, getWalletLogPresentation } from './walletLogPresentation';

const baseLog: WalletLog = {
  walletType: 'UNIFIED',
  tenantId: null,
  bizType: 'SALES_ORDER',
  bizNo: 'SO202607060001',
  changeAmount: -1200,
  balanceBefore: 5000,
  balanceAfter: 3800,
  remark: '订单消费扣减',
  createTime: '2026-07-06T10:00:00',
};

describe('walletLogPresentation', () => {
  it('explains order payment logs and links back to order detail', () => {
    const presentation = getWalletLogPresentation(baseLog);

    expect(presentation.title).toBe('订单支付');
    expect(presentation.direction).toBe('expense');
    expect(presentation.amountText).toBe('-¥1,200.00');
    expect(presentation.balanceText).toBe('余额 ¥5,000.00 -> ¥3,800.00');
    expect(presentation.source).toContain('业务号 SO202607060001');
    expect(presentation.actionPath).toBe('/order/SO202607060001');
  });

  it('explains recharge logs with positive amount and payment status entry', () => {
    const presentation = getWalletLogPresentation({
      ...baseLog,
      bizType: 'UNIFIED_RECHARGE',
      bizNo: 'WR202607060001',
      changeAmount: 2000,
      balanceBefore: 1000,
      balanceAfter: 3000,
      remark: '统一钱包充值',
    });

    expect(presentation.title).toBe('统一钱包充值');
    expect(presentation.direction).toBe('income');
    expect(presentation.amountText).toBe('+¥2,000.00');
    expect(presentation.actionPath).toBe('/payment/status?bizNo=WR202607060001&source=recharge');
  });

  it('keeps refund source visible even when it cannot infer a concrete refund page', () => {
    const presentation = getWalletLogPresentation({
      ...baseLog,
      bizType: 'MERCHANT_APPROVED_REFUND',
      bizNo: 'RA202607060001',
      changeAmount: 600,
      balanceBefore: 3800,
      balanceAfter: 4400,
      remark: '售后退款到账',
    });

    expect(presentation.title).toBe('售后退款');
    expect(presentation.source).toContain('售后退款到账');
    expect(presentation.actionLabel).toBe('查看售后');
    expect(presentation.actionPath).toBe('/orders');
  });

  it('falls back to readable unknown biz type without losing source details', () => {
    const presentation = getWalletLogPresentation({
      ...baseLog,
      bizType: 'MANUAL_ADJUST',
      bizNo: '',
      changeAmount: 0,
      remark: '客服调账',
    });

    expect(presentation.title).toBe('MANUAL ADJUST');
    expect(presentation.direction).toBe('neutral');
    expect(presentation.source).toContain('客服调账');
    expect(presentation.actionPath).toBeUndefined();
  });

  it('keeps trace actions when building recent wallet entries for the wallet dashboard', () => {
    const entries = buildWalletRecentEntries([
      baseLog,
      {
        ...baseLog,
        bizType: 'UNIFIED_RECHARGE',
        bizNo: 'WR202607060001',
        changeAmount: 2000,
      },
    ]);

    expect(entries[0].actionLabel).toBe('查看订单');
    expect(entries[0].actionPath).toBe('/order/SO202607060001');
    expect(entries[0].percent).toBe('38%');
    expect(entries[1].actionLabel).toBe('查看支付状态');
    expect(entries[1].actionPath).toBe('/payment/status?bizNo=WR202607060001&source=recharge');
  });
});
