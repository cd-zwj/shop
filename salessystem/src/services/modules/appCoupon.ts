import { request } from '../request';
import type { CouponTemplate, UserCoupon, CouponReceiveResult, CouponTimelineEvent } from '../../types/coupon';

type UserCouponStatus = 'USABLE' | 'LOCKED' | 'USED' | 'EXPIRED';
type BackendUserCouponStatus = 'RECEIVED' | 'LOCKED' | 'USED' | 'RELEASED' | 'EXPIRED' | string;

interface BackendUserCoupon {
  id: number;
  couponNo: string;
  templateId: number;
  tenantId: number;
  couponStatus: BackendUserCouponStatus;
  orderNo?: string | null;
  templateName?: string | null;
  couponType?: string | null;
  thresholdAmount?: number | null;
  discountAmount?: number | null;
  discountRate?: number | null;
  maxDiscountAmount?: number | null;
  receiveTime?: string | null;
  expireTime?: string | null;
  useTime?: string | null;
  trace?: import('../../types/wallet').AssetTracePresentation | null;
  timeline?: CouponTimelineEvent[] | null;
}

export const appCouponService = {
  /** 可领取优惠券列表 */
  getAvailableCoupons(tenantId: number) {
    return request<CouponTemplate[]>({
      url: `/v1/app/tenants/${tenantId}/coupons/available`,
      method: 'get',
      authRole: 'user',
    });
  },

  /** 我的优惠券 */
  async getMyCoupons(tenantId: number, status?: UserCouponStatus) {
    const coupons = await request<BackendUserCoupon[]>({
      url: `/v1/app/tenants/${tenantId}/coupons`,
      method: 'get',
      params: status ? { status: toBackendUserCouponStatus(status) } : {},
      authRole: 'user',
    });
    return coupons.map(normalizeUserCoupon);
  },

  /** 领取优惠券 */
  claimCoupon(tenantId: number, templateId: number) {
    return request<CouponReceiveResult>({
      url: `/v1/app/tenants/${tenantId}/coupons/${templateId}/receive`,
      method: 'post',
      authRole: 'user',
    });
  },
};
export default appCouponService;

function toBackendUserCouponStatus(status: UserCouponStatus) {
  if (status === 'USABLE') {
    return 'RECEIVED';
  }
  return status;
}

function normalizeUserCoupon(coupon: BackendUserCoupon): UserCoupon {
  return {
    id: coupon.id,
    couponNo: coupon.couponNo,
    couponTemplateId: coupon.templateId,
    tenantId: coupon.tenantId,
    status: toFrontendUserCouponStatus(coupon.couponStatus),
    name: coupon.templateName || '优惠券',
    couponType: toFrontendCouponType(coupon.couponType),
    thresholdAmount: Number(coupon.thresholdAmount ?? 0),
    discountAmount: coupon.discountAmount ?? null,
    discountRate: coupon.discountRate ?? null,
    maxDiscountAmount: coupon.maxDiscountAmount ?? null,
    receiveTime: coupon.receiveTime || '',
    expireTime: coupon.expireTime || '',
    usedTime: coupon.useTime ?? null,
    orderNo: coupon.orderNo ?? null,
    trace: coupon.trace ?? null,
    timeline: coupon.timeline ?? [],
  };
}

function toFrontendUserCouponStatus(status: BackendUserCouponStatus): UserCoupon['status'] {
  if (status === 'USED') {
    return 'USED';
  }
  if (status === 'LOCKED') {
    return 'LOCKED';
  }
  if (status === 'EXPIRED') {
    return 'EXPIRED';
  }
  return 'USABLE';
}

function toFrontendCouponType(type?: string | null): UserCoupon['couponType'] {
  if (type === 'DISCOUNT_RATE' || type === 'RATE') {
    return 'RATE';
  }
  return 'FIXED';
}
