import { describe, expect, it } from 'vitest';
import type { MerchantTransaction } from '../types/merchant';
import { summarizeMerchantTransactions } from './merchantFinancePresentation';

function tx(overrides: Partial<MerchantTransaction>): MerchantTransaction {
  return {
    id: overrides.id ?? Math.random(),
    bizType: overrides.bizType ?? 'PAYMENT',
    changeAmount: overrides.changeAmount ?? 0,
    ...overrides,
  };
}

describe('summarizeMerchantTransactions', () => {
  it('summarizes payment, recharge, refund, withdrawal and net change', () => {
    const summary = summarizeMerchantTransactions([
      tx({ bizType: 'PAYMENT', changeAmount: 120 }),
      tx({ bizType: 'PAYMENT', changeAmount: -10 }),
      tx({ bizType: 'RECHARGE', changeAmount: 50 }),
      tx({ bizType: 'REFUND', changeAmount: -35 }),
      tx({ bizType: 'WITHDRAWAL', changeAmount: -80 }),
      tx({ bizType: 'POINTS', changeAmount: 6 }),
    ]);

    expect(summary).toEqual({
      paymentIncome: 120,
      rechargeIncome: 50,
      refundAmount: 35,
      withdrawalAmount: 80,
      otherChange: -4,
      netChange: 51,
      transactionCount: 6,
    });
  });

  it('treats malformed amounts as zero', () => {
    const summary = summarizeMerchantTransactions([
      tx({ bizType: 'PAYMENT', changeAmount: Number.NaN }),
      tx({ bizType: 'REFUND', changeAmount: Number.POSITIVE_INFINITY }),
    ]);

    expect(summary.paymentIncome).toBe(0);
    expect(summary.refundAmount).toBe(0);
    expect(summary.netChange).toBe(0);
  });

  it('handles empty transaction lists', () => {
    expect(summarizeMerchantTransactions([])).toEqual({
      paymentIncome: 0,
      rechargeIncome: 0,
      refundAmount: 0,
      withdrawalAmount: 0,
      otherChange: 0,
      netChange: 0,
      transactionCount: 0,
    });
  });
});
