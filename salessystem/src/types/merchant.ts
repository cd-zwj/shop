import type { SalesOrder, SalesOrderDetail } from './order';

export interface MerchantProduct {
  id: number;
  tenantId: number;
  productCode: string;
  name: string;
  price: number;
  unit?: string | null;
  category?: string | null;
  description?: string | null;
  imageUrl?: string | null;
  stock: number;
  status: 'active' | 'inactive' | 'out_of_stock' | string;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface MerchantProductUpsertPayload {
  productCode?: string;
  name: string;
  price: number;
  unit?: string;
  category?: string;
  description?: string;
  imageUrl?: string;
  stock: number;
  status?: 'active' | 'inactive' | 'out_of_stock';
}

export interface MerchantProductFilters {
  current?: number;
  size?: number;
  search?: string;
  category?: string;
  status?: string;
}

export interface MerchantOrderFilters {
  current?: number;
  size?: number;
  orderStatus?: string;
  payStatus?: string;
  keyword?: string;
}

export interface MerchantWalletSummary {
  tenantId: number;
  availableBalance: number;
  frozenBalance: number;
  totalIncome: number;
  totalWithdrawal: number;
}

export interface MerchantPointsRule {
  pointsRatio: number;
  enabled: boolean;
}

export interface MerchantPointsRulePayload {
  pointsRatio: number;
  enabled: boolean;
}

export interface MerchantRechargeRule {
  id?: number;
  tenantId?: number;
  rechargeAmount: number;
  giftAmount: number;
  giftPoints: number;
  status: number;
  sortOrder?: number | null;
}

export interface MerchantRechargeRulePayload {
  id?: number;
  rechargeAmount: number;
  giftAmount: number;
  giftPoints: number;
  enabled: boolean;
  sortOrder?: number;
}

export interface MerchantWithdrawal {
  id: number;
  tenantId: number;
  amount: number;
  bankName: string;
  bankAccount: string;
  accountName: string;
  status: number;
  rejectReason?: string | null;
  applyTime?: string | null;
  approveTime?: string | null;
  approverId?: number | null;
  createTime?: string | null;
}

export interface MerchantWithdrawalFilters {
  current?: number;
  size?: number;
  status?: number;
}

export interface MerchantWithdrawalApplyPayload {
  amount: number;
  bankName: string;
  bankAccount: string;
  accountName: string;
}

export type MerchantOrder = SalesOrder;
export type MerchantOrderDetail = SalesOrderDetail;
