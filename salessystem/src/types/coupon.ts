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
  status: 'USABLE' | 'USED' | 'EXPIRED';
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
}

export interface CouponReceiveResult {
  userCouponId: number;
  couponNo: string;
  couponTemplateId: number;
  tenantId: number;
  status: string;
  expireTime: string;
}
