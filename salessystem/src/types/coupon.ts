import type { AssetTracePresentation } from './wallet';

export interface CouponTemplate {
  id: number;
  tenantId: number;
  ownerType: 'PLATFORM' | 'TENANT';
  name: string;
  couponType: 'FIXED' | 'RATE';
  thresholdAmount: number; // 元
  discountAmount: number | null;
  discountRate: number | null;
  maxDiscountAmount: number | null;
  perUserLimit: number;
  remainingStock: number;
  receivedByCurrentUser: number;
  receivable: boolean;
  receiveStartTime: string;
  receiveEndTime: string;
  validStartTime: string | null;
  validEndTime: string | null;
  validDaysAfterReceive: number | null;
  description: string | null;
}

export interface UserCoupon {
  id: number;
  couponNo: string;
  couponTemplateId: number;
  tenantId: number;
  status: 'USABLE' | 'LOCKED' | 'USED' | 'EXPIRED';
  name: string;
  couponType: 'FIXED' | 'RATE';
  thresholdAmount: number;
  discountAmount: number | null;
  discountRate: number | null;
  maxDiscountAmount: number | null;
  receiveTime: string;
  expireTime: string;
  usedTime: string | null;
  orderNo?: string | null;
  trace?: AssetTracePresentation | null;
  timeline?: CouponTimelineEvent[];
}

export interface CouponTimelineEvent {
  eventType: 'RECEIVE' | 'LOCK' | 'RELEASE' | 'WRITE_OFF' | 'EXPIRE' | string;
  title: string;
  description?: string | null;
  occurredAt?: string | null;
  orderNo?: string | null;
  bizNo?: string | null;
  amount?: number | null;
  status?: string | null;
}

export interface CouponReceiveResult {
  userCouponId: number;
  couponNo: string;
  couponTemplateId: number;
  tenantId: number;
  status: string;
  expireTime: string;
}
