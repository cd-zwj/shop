import type { MerchantTransaction, MerchantWalletSummary, MerchantWithdrawal } from '../types/merchant';

export interface MerchantTransactionSummary {
  paymentIncome: number;
  rechargeIncome: number;
  refundAmount: number;
  withdrawalAmount: number;
  otherChange: number;
  netChange: number;
  transactionCount: number;
}

export type MerchantReconciliationStatus = 'balanced' | 'attention' | 'risk';

export interface MerchantReconciliation {
  ledgerBalance: number;
  retainedAmount: number;
  adjustmentAmount: number;
  pendingWithdrawalAmount: number;
  currentPageNetChange: number;
  status: MerchantReconciliationStatus;
  statusLabel: string;
  statusDescription: string;
  riskItems: string[];
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

export function buildMerchantReconciliation(
  summary: MerchantWalletSummary,
  withdrawals: MerchantWithdrawal[],
  transactionSummary: MerchantTransactionSummary,
): MerchantReconciliation {
  const availableBalance = normalizeAmount(summary.availableBalance);
  const frozenBalance = normalizeAmount(summary.frozenBalance);
  const totalIncome = normalizeAmount(summary.totalIncome);
  const totalWithdrawal = normalizeAmount(summary.totalWithdrawal);
  const ledgerBalance = roundMoney(availableBalance + frozenBalance);
  const retainedAmount = roundMoney(totalIncome - totalWithdrawal);
  const adjustmentAmount = roundMoney(retainedAmount - ledgerBalance);
  const pendingWithdrawalAmount = roundMoney(
    withdrawals
      .filter((withdrawal) => withdrawal.status === 0)
      .reduce((sum, withdrawal) => sum + normalizeAmount(withdrawal.amount), 0),
  );
  const riskItems: string[] = [];

  if (adjustmentAmount < -0.01) {
    riskItems.push('账面余额高于累计收入扣除提现后的留存金额，需要核对入账、退款或人工调整记录。');
  } else if (adjustmentAmount > 0.01) {
    riskItems.push('累计收入扣除提现后与账面余额存在差额，通常来自退款、冲正或人工调整。');
  }

  if (Math.abs(pendingWithdrawalAmount - frozenBalance) > 0.01) {
    riskItems.push('待审核提现金额与冻结余额不一致，请核对提现冻结或解冻记录。');
  }

  const status = riskItems.some((item) => item.startsWith('账面余额高于'))
    ? 'risk'
    : riskItems.length > 0
      ? 'attention'
      : 'balanced';

  return {
    ledgerBalance,
    retainedAmount,
    adjustmentAmount,
    pendingWithdrawalAmount,
    currentPageNetChange: roundMoney(transactionSummary.netChange),
    status,
    statusLabel: getReconciliationStatusLabel(status),
    statusDescription: getReconciliationStatusDescription(status),
    riskItems,
  };
}

function normalizeAmount(value: number | string | null | undefined) {
  const amount = Number(value ?? 0);
  return Number.isFinite(amount) ? amount : 0;
}

function roundMoney(value: number) {
  return Math.round(value * 100) / 100;
}

function getReconciliationStatusLabel(status: MerchantReconciliationStatus) {
  if (status === 'risk') {
    return '账务需排查';
  }
  if (status === 'attention') {
    return '存在调整差额';
  }
  return '账务口径可核对';
}

function getReconciliationStatusDescription(status: MerchantReconciliationStatus) {
  if (status === 'risk') {
    return '余额、收入、提现之间存在反向差异，需要优先核对资金流水。';
  }
  if (status === 'attention') {
    return '余额口径存在差额，通常来自退款、冲正或人工调整，请结合流水继续核对。';
  }
  return '可提现、冻结、累计收入和提现之间的基础核对关系正常。';
}
