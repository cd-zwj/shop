import { request } from '../request';
import type {
  MerchantCouponTemplate,
  CouponTemplateCreatePayload,
  CouponScope,
  CouponScopeCreatePayload,
  PromotionActivity,
  ActivityRule,
  ActivityRuleCreatePayload,
} from '../../types/marketing';

export const adminMarketingService = {
  // Coupon Templates
  getCouponTemplates(status?: string) {
    return request<MerchantCouponTemplate[]>({
      url: `/v1/admin/marketing/coupons`,
      method: 'get',
      params: { status },
      authRole: 'admin',
    });
  },

  createCouponTemplate(payload: CouponTemplateCreatePayload) {
    return request<MerchantCouponTemplate>({
      url: `/v1/admin/marketing/coupons`,
      method: 'post',
      data: payload,
      authRole: 'admin',
    });
  },

  activateCoupon(templateId: number) {
    return request<void>({
      url: `/v1/admin/marketing/coupons/${templateId}/activate`,
      method: 'put',
      authRole: 'admin',
    });
  },

  disableCoupon(templateId: number) {
    return request<void>({
      url: `/v1/admin/marketing/coupons/${templateId}/disable`,
      method: 'put',
      authRole: 'admin',
    });
  },

  getCouponScopes(templateId: number) {
    return request<CouponScope[]>({
      url: `/v1/admin/marketing/coupons/${templateId}/scopes`,
      method: 'get',
      authRole: 'admin',
    });
  },

  addCouponScope(templateId: number, payload: CouponScopeCreatePayload) {
    return request<CouponScope>({
      url: `/v1/admin/marketing/coupons/${templateId}/scopes`,
      method: 'post',
      data: payload,
      authRole: 'admin',
    });
  },

  // Promotion Activities
  getActivities(status?: string) {
    return request<PromotionActivity[]>({
      url: `/v1/admin/marketing/activities`,
      method: 'get',
      params: { status },
      authRole: 'admin',
    });
  },

  createActivity(payload: { name: string; activityType: string; startTime: string; endTime: string; description?: string }) {
    return request<PromotionActivity>({
      url: `/v1/admin/marketing/activities`,
      method: 'post',
      data: payload,
      authRole: 'admin',
    });
  },

  getActivityRules(activityId: number) {
    return request<ActivityRule[]>({
      url: `/v1/admin/marketing/activities/${activityId}/rules`,
      method: 'get',
      authRole: 'admin',
    });
  },

  addActivityRule(activityId: number, payload: ActivityRuleCreatePayload) {
    return request<ActivityRule>({
      url: `/v1/admin/marketing/activities/${activityId}/rules`,
      method: 'post',
      data: payload,
      authRole: 'admin',
    });
  },

  activateActivity(activityId: number) {
    return request<void>({
      url: `/v1/admin/marketing/activities/${activityId}/activate`,
      method: 'put',
      authRole: 'admin',
    });
  },

  disableActivity(activityId: number) {
    return request<void>({
      url: `/v1/admin/marketing/activities/${activityId}/disable`,
      method: 'put',
      authRole: 'admin',
    });
  },
};
