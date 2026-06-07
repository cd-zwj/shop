import { request } from '../request';
import type {
  MerchantCouponTemplate,
  CouponTemplateCreatePayload,
  CouponScope,
  CouponScopeCreatePayload,
  PromotionActivity,
  ActivityRule,
  ActivityRuleCreatePayload,
  MemberLevel,
  MemberTag,
} from '../../types/marketing';

export const merchantMarketingService = {
  // Coupon Templates
  getCouponTemplates(tenantId: number, status?: string) {
    return request<MerchantCouponTemplate[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons`,
      method: 'get',
      params: { status },
      authRole: 'merchant',
    });
  },

  createCouponTemplate(tenantId: number, payload: CouponTemplateCreatePayload) {
    return request<MerchantCouponTemplate>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  activateCoupon(tenantId: number, templateId: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons/${templateId}/activate`,
      method: 'put',
      authRole: 'merchant',
    });
  },

  disableCoupon(tenantId: number, templateId: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons/${templateId}/disable`,
      method: 'put',
      authRole: 'merchant',
    });
  },

  getCouponScopes(tenantId: number, templateId: number) {
    return request<CouponScope[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons/${templateId}/scopes`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  addCouponScope(tenantId: number, templateId: number, payload: CouponScopeCreatePayload) {
    return request<CouponScope>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons/${templateId}/scopes`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  // Promotion Activities
  getActivities(tenantId: number, status?: string) {
    return request<PromotionActivity[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities`,
      method: 'get',
      params: { status },
      authRole: 'merchant',
    });
  },

  createActivity(tenantId: number, payload: { name: string; activityType: string; startTime: string; endTime: string; description?: string }) {
    return request<PromotionActivity>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  getActivityRules(tenantId: number, activityId: number) {
    return request<ActivityRule[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities/${activityId}/rules`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  addActivityRule(tenantId: number, activityId: number, payload: ActivityRuleCreatePayload) {
    return request<ActivityRule>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities/${activityId}/rules`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  activateActivity(tenantId: number, activityId: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities/${activityId}/activate`,
      method: 'put',
      authRole: 'merchant',
    });
  },

  disableActivity(tenantId: number, activityId: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities/${activityId}/disable`,
      method: 'put',
      authRole: 'merchant',
    });
  },

  // Member Levels & Tags
  getMemberLevels(tenantId: number) {
    return request<MemberLevel[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/member-levels`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  createMemberLevel(tenantId: number, params: { level: number; name: string; thresholdAmount: number; discountRate: number }) {
    return request<MemberLevel>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/member-levels`,
      method: 'post',
      params, // query params, since backend uses @RequestParam
      authRole: 'merchant',
    });
  },

  getMemberTags(tenantId: number) {
    return request<MemberTag[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/member-tags`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  createMemberTag(tenantId: number, name: string) {
    return request<MemberTag>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/member-tags`,
      method: 'post',
      params: { name }, // query params, since backend uses @RequestParam
      authRole: 'merchant',
    });
  },
};
