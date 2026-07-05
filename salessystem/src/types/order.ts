import type { PaymentChannelCode } from './payment';

export type WalletStrategy =
  | 'NO_WALLET'
  | 'UNIFIED_ONLY'
  | 'MERCHANT_ONLY'
  | 'MERCHANT_THEN_UNIFIED'
  | 'UNIFIED_THEN_MERCHANT'
  | 'CUSTOM_SPLIT';

export interface AppCreateOrderItemPayload {
  productId: number;
  quantity: number;
  price: number;
}

export interface AppCreateOrderPayload {
  tenantId: number;
  totalAmount: number;
  subject?: string;
  source?: string;
  items: AppCreateOrderItemPayload[];
  walletStrategy: WalletStrategy;
  paymentChannelCode?: PaymentChannelCode;
  unifiedWalletAmount?: number;
  merchantWalletAmount?: number;
  allowExternalPayFallback?: boolean;
  selectedUserCouponId?: number;
}

export interface OrderPayment {
  orderNo: string;
  orderStatus: string;
  payStatus: string;
  totalAmount: number;
  unifiedWalletDeductAmount: number;
  merchantWalletDeductAmount: number;
  externalPayAmount: number;
  paymentBillNo?: string | null;
  externalPayUrl?: string | null;
  reusedPaymentBill?: boolean | null;
}

export interface SalesOrder {
  id: number;
  orderNo: string;
  tenantId: number;
  platformUserId: number;
  orderStatus: string;
  payStatus: string;
  totalAmount: number;
  discountAmount?: number | null;
  walletDeductAmount?: number | null;
  unifiedWalletDeductAmount?: number | null;
  merchantWalletDeductAmount?: number | null;
  externalPayAmount?: number | null;
  payableAmount?: number | null;
  subject?: string | null;
  source?: string | null;
  walletStrategy?: string | null;
  expireTime?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface SalesOrderItem {
  id: number;
  orderId: number;
  orderNo: string;
  tenantId: number;
  productId: number;
  productName: string;
  price: number;
  quantity: number;
  subtotal: number;
  /** PHYSICAL / VIRTUAL / CARD_KEY / SERVICE / SUBSCRIPTION */
  productType?: string | null;
  /** PENDING / DELIVERING / DELIVERED / CONFIRMED / FAILED / REVOKED */
  deliveryStatus?: string | null;
  deliveredTime?: string | null;
  createTime?: string | null;
}

export interface SalesOrderDetail {
  order: SalesOrder;
  items: SalesOrderItem[];
  paymentBillNo?: string | null;
  paymentBillStatus?: string | null;
  paymentBillStatusRemark?: string | null;
  paymentBillExpireTime?: string | null;
}
