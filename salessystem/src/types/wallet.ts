import type { PaymentChannelCode } from './payment';

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

export interface UnifiedWalletRechargePayload {
  amount: number;
  paymentChannelCode: PaymentChannelCode;
}

export interface MerchantWalletRechargePayload {
  ruleId: number;
  paymentChannelCode: PaymentChannelCode;
}

