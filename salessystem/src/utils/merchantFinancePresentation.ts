import type { MerchantTransaction } from '../types/merchant';

export interface MerchantTransactionSummary {
  paymentIncome: number;
  rechargeIncome: number;
  refundAmount: number;
  withdrawalAmount: number;
  otherChange: number;
  netChange: number;
  transactionCount: number;
}

const EMPTY_TRANSACTION_SUMMARY: MerchantTransactionSummary = {
  paymentIncome: 0,
  rechargeIncome: 0,
  refundAmount: 0,
  withdrawalAmount: 0,
  otherChange: 0,
  netChange: 0,
  transactionCount: 0,
};

export function summarizeMerchantTransactions(
  transactions: MerchantTransaction[],
): MerchantTransactionSummary {
  return transactions.reduce<MerchantTransactionSummary>((summary, transaction) => {
    const amount = normalizeAmount(transaction.changeAmount);
    const absoluteAmount = Math.abs(amount);

    if (transaction.bizType === 'PAYMENT' && amount > 0) {
      return {
        ...summary,
        paymentIncome: summary.paymentIncome + amount,
        netChange: summary.netChange + amount,
        transactionCount: summary.transactionCount + 1,
      };
    }

    if (transaction.bizType === 'RECHARGE' && amount > 0) {
      return {
        ...summary,
        rechargeIncome: summary.rechargeIncome + amount,
        netChange: summary.netChange + amount,
        transactionCount: summary.transactionCount + 1,
      };
    }

    if (transaction.bizType === 'REFUND' && amount < 0) {
      return {
        ...summary,
        refundAmount: summary.refundAmount + absoluteAmount,
        netChange: summary.netChange + amount,
        transactionCount: summary.transactionCount + 1,
      };
    }

    if (transaction.bizType === 'WITHDRAWAL' && amount < 0) {
      return {
        ...summary,
        withdrawalAmount: summary.withdrawalAmount + absoluteAmount,
        netChange: summary.netChange + amount,
        transactionCount: summary.transactionCount + 1,
      };
    }

    return {
      ...summary,
      otherChange: summary.otherChange + amount,
      netChange: summary.netChange + amount,
      transactionCount: summary.transactionCount + 1,
    };
  }, EMPTY_TRANSACTION_SUMMARY);
}

function normalizeAmount(value: number | string | null | undefined) {
  const amount = Number(value ?? 0);
  return Number.isFinite(amount) ? amount : 0;
}
