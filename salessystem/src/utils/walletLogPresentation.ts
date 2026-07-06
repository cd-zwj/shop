import type { WalletLog } from '../types/wallet';
import { formatCurrency } from './display';

export type WalletLogDirection = 'income' | 'expense' | 'neutral';

export interface WalletLogPresentation {
  title: string;
  source: string;
  amountText: string;
  balanceText: string;
  direction: WalletLogDirection;
  badgeClass: string;
  initials: string;
  actionLabel?: string;
  actionPath?: string;
}

export interface WalletRecentEntry extends WalletLogPresentation {
  percent: string;
  color: string;
}

const BIZ_TYPE_LABELS: Record<string, string> = {
  SALES_ORDER: '订单支付',
  ORDER_DEDUCT: '积分抵扣',
  ORDER_REWARD: '消费返积分',
  ORDER_CANCEL_REFUND: '订单取消退款',
  UNIFIED_RECHARGE: '统一钱包充值',
  MERCHANT_RECHARGE: '商户钱包充值',
  RECHARGE: '充值',
  REFUND: '退款',
  MERCHANT_APPROVED_REFUND: '售后退款',
  LATE_CALLBACK_REFUND: '异常支付退款',
};

export function getWalletLogPresentation(log: WalletLog): WalletLogPresentation {
  const amount = Number(log.changeAmount || 0);
  const direction = getDirection(amount);
  const label = getBizTypeLabel(log.bizType);
  const source = getSourceText(log);
  const action = getWalletLogAction(log);

  return {
    title: label,
    source,
    amountText: `${amount > 0 ? '+' : ''}${formatCurrency(amount)}`,
    balanceText: `余额 ${formatCurrency(log.balanceBefore)} -> ${formatCurrency(log.balanceAfter)}`,
    direction,
    badgeClass: getBadgeClass(direction),
    initials: getInitials(log.bizType, direction),
    actionLabel: action?.label,
    actionPath: action?.path,
  };
}

export function buildWalletRecentEntries(logs: WalletLog[]): WalletRecentEntry[] {
  if (logs.length === 0) {
    return [];
  }

  const totalChange = logs.reduce((sum, log) => sum + Math.abs(Number(log.changeAmount || 0)), 0) || 1;
  return logs.slice(0, 3).map((log, index) => {
    const presentation = getWalletLogPresentation(log);
    return {
      ...presentation,
      percent: `${Math.round((Math.abs(Number(log.changeAmount || 0)) / totalChange) * 100)}%`,
      color: index === 0 ? 'bg-primary/10 text-primary' : presentation.badgeClass,
    };
  });
}

function getDirection(amount: number): WalletLogDirection {
  if (amount > 0) {
    return 'income';
  }
  if (amount < 0) {
    return 'expense';
  }
  return 'neutral';
}

function getBizTypeLabel(bizType?: string | null) {
  if (!bizType) {
    return '钱包流水';
  }
  return BIZ_TYPE_LABELS[bizType] ?? bizType.replaceAll('_', ' ');
}

function getSourceText(log: WalletLog) {
  const parts = [
    log.walletType === 'MERCHANT' && log.tenantId ? `商户 #${log.tenantId}` : log.walletType === 'UNIFIED' ? '统一钱包' : log.walletType,
    log.bizNo ? `业务号 ${log.bizNo}` : null,
    log.remark,
  ].filter(Boolean);
  return parts.join(' · ') || '来源待补充';
}

function getWalletLogAction(log: WalletLog): { label: string; path: string } | null {
  if (!log.bizNo) {
    return null;
  }

  if (log.bizType === 'SALES_ORDER' || log.bizType === 'ORDER_CANCEL_REFUND') {
    return { label: '查看订单', path: `/order/${encodeURIComponent(log.bizNo)}` };
  }

  if (log.bizType === 'MERCHANT_APPROVED_REFUND' || log.bizType === 'LATE_CALLBACK_REFUND' || log.bizType === 'REFUND') {
    return { label: '查看售后', path: '/orders' };
  }

  if (log.bizType === 'UNIFIED_RECHARGE' || log.bizType === 'MERCHANT_RECHARGE' || log.bizType === 'RECHARGE') {
    return { label: '查看支付状态', path: `/payment/status?bizNo=${encodeURIComponent(log.bizNo)}&source=recharge` };
  }

  return null;
}

function getBadgeClass(direction: WalletLogDirection) {
  if (direction === 'income') {
    return 'bg-emerald-50 text-emerald-700';
  }
  if (direction === 'expense') {
    return 'bg-rose-50 text-rose-700';
  }
  return 'bg-slate-100 text-slate-600';
}

function getInitials(bizType: string | null | undefined, direction: WalletLogDirection) {
  if (!bizType) {
    return direction === 'income' ? 'IN' : direction === 'expense' ? 'OUT' : 'WL';
  }
  return bizType
    .split('_')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 3)
    .toUpperCase();
}
