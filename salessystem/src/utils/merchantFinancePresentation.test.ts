import { describe, expect, it } from 'vitest';
import type { MerchantTransaction, MerchantWalletSummary, MerchantWithdrawal } from '../types/merchant';
import {
  buildMerchantReconciliation,
  summarizeMerchantTransactions,
} from './merchantFinancePresentation';

function tx(overrides: Partial<MerchantTransaction>): MerchantTransaction {
  return {
    id: overrides.id ?? Math.random(),
    bizType: overrides.bizType ?? 'PAYMENT',
    changeAmount: overrides.changeAmount ?? 0,
    feeAmount: overrides.feeAmount ?? null,
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
      platformFeeIncome: 0,
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
      platformFeeIncome: 0,
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

describe('buildMerchantReconciliation', () => {
  it('builds a balanced reconciliation snapshot with pending withdrawals', () => {
    const reconciliation = buildMerchantReconciliation(
      walletSummary({
        availableBalance: 700,
        frozenBalance: 100,
        totalIncome: 1000,
        totalWithdrawal: 200,
      }),
      [
        withdrawal({ id: 1, status: 0, amount: 100 }),
        withdrawal({ id: 2, status: 1, amount: 200 }),
      ],
      summarizeMerchantTransactions([
        tx({ bizType: 'PAYMENT', changeAmount: 300 }),
        tx({ bizType: 'REFUND', changeAmount: -40 }),
      ]),
    );

    expect(reconciliation).toMatchObject({
      ledgerBalance: 800,
      retainedAmount: 800,
      adjustmentAmount: 0,
      pendingWithdrawalAmount: 100,
      status: 'balanced',
      statusLabel: '账务口径可核对',
    });
    expect(reconciliation.riskItems).toEqual([]);
  });

  it('surfaces refund or adjustment difference without treating it as broken', () => {
    const reconciliation = buildMerchantReconciliation(
      walletSummary({
        availableBalance: 500,
        frozenBalance: 0,
        totalIncome: 1000,
        totalWithdrawal: 200,
      }),
      [],
      summarizeMerchantTransactions([
        tx({ bizType: 'REFUND', changeAmount: -300 }),
      ]),
    );

    expect(reconciliation.adjustmentAmount).toBe(300);
    expect(reconciliation.status).toBe('attention');
    expect(reconciliation.riskItems).toContain('累计收入扣除提现后与账面余额存在差额，通常来自退款、冲正或人工调整。');
  });

  it('flags impossible balance states and pending withdrawal mismatch', () => {
    const reconciliation = buildMerchantReconciliation(
      walletSummary({
        availableBalance: 900,
        frozenBalance: 50,
        totalIncome: 1000,
        totalWithdrawal: 200,
      }),
      [withdrawal({ id: 1, status: 0, amount: 80 })],
      summarizeMerchantTransactions([]),
    );

    expect(reconciliation.status).toBe('risk');
    expect(reconciliation.riskItems).toContain('账面余额高于累计收入扣除提现后的留存金额，需要核对入账、退款或人工调整记录。');
    expect(reconciliation.riskItems).toContain('待审核提现金额与冻结余额不一致，请核对提现冻结或解冻记录。');
  });
});

function walletSummary(overrides: Partial<MerchantWalletSummary>): MerchantWalletSummary {
  return {
    tenantId: 9,
    availableBalance: 0,
    frozenBalance: 0,
    totalIncome: 0,
    totalWithdrawal: 0,
    totalPlatformFee: 0,
    ...overrides,
  };
}

function withdrawal(overrides: Partial<MerchantWithdrawal>): MerchantWithdrawal {
  return {
    id: overrides.id ?? Math.random(),
    tenantId: 9,
    amount: 0,
    bankName: '测试银行',
    bankAccount: '6222000000000000',
    accountName: '测试商户',
    status: 0,
    ...overrides,
  };
}
