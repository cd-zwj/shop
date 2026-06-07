import { request } from '../request';
import type { CouponTemplate, UserCoupon, CouponReceiveResult } from '../../types/coupon';

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
  getMyCoupons(tenantId: number, status?: 'USABLE' | 'USED' | 'EXPIRED') {
    return request<UserCoupon[]>({
      url: `/v1/app/tenants/${tenantId}/coupons`,
      method: 'get',
      params: status ? { status } : {},
      authRole: 'user',
    });
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
