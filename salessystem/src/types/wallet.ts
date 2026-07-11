import type { PaymentChannelCode } from './payment';

export type AssetTraceTone = 'positive' | 'negative' | 'neutral';

export interface AssetTracePresentation {
  title?: string | null;
  source?: string | null;
  effect?: string | null;
  balance?: string | null;
  status?: string | null;
  hint?: string | null;
  actionLabel?: string | null;
  actionPath?: string | null;
  inactiveActionLabel?: string | null;
  tone?: AssetTraceTone | null;
}

export interface WalletAccount {
  walletType: string;
  tenantId: number | null;
  availableAmount: number;
  frozenAmount: number;
  totalRecharge: number;
  totalConsume: number;
}

export interface WalletLog {
  walletType: string;
  tenantId: number | null;
  bizType: string;
  bizNo: string;
  changeAmount: number;
  balanceBefore: number;
  balanceAfter: number;
  remark?: string | null;
  createTime?: string | null;
  trace?: AssetTracePresentation | null;
}

export interface TenantAssetSummary {
  tenantId: number;
  tenantName: string;
  memberStatus?: number | null;
  walletAvailableAmount: number;
  walletFrozenAmount: number;
  points: number;
  expiringSoonPoints: number;
  usableCouponCount?: number;
  lockedCouponCount?: number;
  usedCouponCount?: number;
  expiredCouponCount?: number;
  expiringSoonCouponCount?: number;
  totalGrowth?: number;
}

export interface AssetActivity {
  assetType: 'WALLET' | 'POINTS' | 'GROWTH' | 'COUPON' | string;
  title: string;
  description?: string | null;
  occurredAt?: string | null;
  tenantId?: number | null;
  tenantName?: string | null;
  bizNo?: string | null;
  amountText?: string | null;
  tone?: AssetTraceTone | null;
  actionPath?: string | null;
}

export interface RechargePayment {
  rechargeNo: string;
  walletType: string;
  tenantId: number | null;
  rechargeAmount: number;
  giftAmount: number;
  giftPoints: number;
  paymentBillNo: string;
  externalPayUrl?: string | null;
}

export interface MerchantRechargeRule {
  id: number;
  tenantId: number;
  rechargeAmount: number;
  giftAmount: number;
  giftPoints: number;
  status: number;
  sortOrder?: number | null;
}

/**
 * 统一钱包充值规则（用户端 API 返回，已隐藏内部字段，金额单位：分）。
 */
export interface UnifiedRechargeRule {
  id: number;
  rechargeAmount: number;
  giftAmount: number;
  giftPoints: number;
  sortOrder?: number | null;
}

export interface UnifiedWalletRechargePayload {
  amount: number;
  paymentChannelCode: PaymentChannelCode;
}

export interface MerchantWalletRechargePayload {
  ruleId: number;
  paymentChannelCode: PaymentChannelCode;
}

